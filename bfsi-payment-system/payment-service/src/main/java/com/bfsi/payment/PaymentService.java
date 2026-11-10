package com.bfsi.payment;

import com.bfsi.payment.Clients.*;
import com.bfsi.payment.Dtos.*;
import com.bfsi.payment.Enums.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {
    private static final Pattern IFSC = Pattern.compile("^[A-Z]{4}0[A-Z0-9]{6}$");
    private static final Pattern VPA = Pattern.compile("^[\\w.\\-]{2,}@[a-zA-Z]{2,}$");
    private static final Pattern DIGITS_ACC = Pattern.compile("^\\d{9,18}$");
    private static final Pattern LAST4 = Pattern.compile("^\\d{4}$");
    private static final String ALNUM = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RNG = new SecureRandom();

    private final PaymentRepository payments;
    private final CustomerClient customers;
    private final LedgerClient ledger;
    private final NotificationClient notifications;

    public Payment process(Long userId, String email, String idemKey, PaymentRequest r) {
        if (idemKey != null && !idemKey.isBlank()) {
            var existing = payments.findByUserIdAndIdempotencyKey(userId, idemKey);
            if (existing.isPresent()) return existing.get();   // safe client retry
        }
        validate(r);

        // 1. Persist intent first so a crash after debit never loses the transaction
        Payment p = new Payment();
        p.setTxnId(genTxnId(r.mode()));
        p.setUserId(userId);
        p.setUserEmail(email);
        p.setFromAccount(r.fromAccount());
        p.setToAccount(r.toAccount().trim());
        p.setAmount(r.amount());
        p.setMode(r.mode());
        p.setIfsc(r.ifsc());
        p.setCardLast4(r.cardLast4());
        p.setRemarks(r.remarks());
        p.setIdempotencyKey(idemKey == null || idemKey.isBlank() ? null : idemKey);
        p.setStatus(PaymentStatus.PENDING);
        p = payments.save(p);

        // 2. Move the money in core banking (customer-service)
        TransferResult tr;
        try {
            tr = customers.transfer(new TransferRequest(userId, p.getFromAccount(), p.getToAccount(), p.getAmount()));
        } catch (Exception e) {
            log.error("Transfer outcome unknown for {}", p.getTxnId(), e);
            p.setFailureReason("Outcome unknown; awaiting reconciliation");
            p.setUpdatedAt(Instant.now());
            safeAudit(email, "PAYMENT_PENDING", p);
            return payments.save(p);
        }

        boolean ok = "SUCCESS".equals(tr.status());
        p.setStatus(ok ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
        p.setFailureReason(ok ? null : tr.reason());
        p.setUpdatedAt(Instant.now());
        p = payments.save(p);

        final Payment fp = p;
        final TransferResult t = tr;
        // 3. Ledger, audit trail and alerts (best effort: never undo a completed payment because of these)
        String desc = fp.getMode() + " to " + fp.getToAccount() + (fp.getRemarks() == null ? "" : " - " + fp.getRemarks());
        if (ok) {
            safe(() -> ledger.record(new LedgerRequest(fp.getTxnId(), userId, fp.getFromAccount(), "DEBIT",
                    fp.getAmount(), t.fromBalanceAfter(), fp.getMode().name(), desc)));
            if (t.toUserId() != null)
                safe(() -> ledger.record(new LedgerRequest(fp.getTxnId(), t.toUserId(), fp.getToAccount(), "CREDIT",
                        fp.getAmount(), t.toBalanceAfter(), fp.getMode().name(), "Received from " + fp.getFromAccount())));
            safe(() -> notifications.send(new NotifyRequest(userId,
                    "INR " + fp.getAmount() + " debited via " + fp.getMode() + ". Txn ID " + fp.getTxnId())));
            if (t.toUserId() != null)
                safe(() -> notifications.send(new NotifyRequest(t.toUserId(),
                        "INR " + fp.getAmount() + " credited to your account. Txn ID " + fp.getTxnId())));
        } else {
            safe(() -> notifications.send(new NotifyRequest(userId,
                    "Payment " + fp.getTxnId() + " failed: " + t.reason())));
        }
        safeAudit(email, "PAYMENT_" + p.getStatus(), p);
        return p;
    }

    private void validate(PaymentRequest r) {
        BigDecimal limit;
        switch (r.mode()) {
            case UPI -> {
                limit = new BigDecimal("100000");
                if (!VPA.matcher(r.toAccount().trim()).matches() && !DIGITS_ACC.matcher(r.toAccount().trim()).matches())
                    bad("UPI payee must be a VPA (name@bank) or an account number");
            }
            case NEFT -> {
                limit = new BigDecimal("1000000");
                if (r.ifsc() == null || !IFSC.matcher(r.ifsc()).matches()) bad("Valid IFSC is required for NEFT");
                if (!DIGITS_ACC.matcher(r.toAccount().trim()).matches()) bad("Beneficiary account must be 9-18 digits");
            }
            default -> {   // CARD
                limit = new BigDecimal("200000");
                if (r.cardLast4() == null || !LAST4.matcher(r.cardLast4()).matches()) bad("cardLast4 (4 digits) is required for CARD");
            }
        }
        if (r.amount().compareTo(limit) > 0) bad(r.mode() + " per-transaction limit is INR " + limit.toPlainString());
    }

    private void bad(String m) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, m); }

    /** e.g. TXN-UPI-20261103101530-K7M2QX */
    static String genTxnId(PaymentMode mode) {
        StringBuilder sb = new StringBuilder("TXN-").append(mode).append('-')
                .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))).append('-');
        for (int i = 0; i < 6; i++) sb.append(ALNUM.charAt(RNG.nextInt(ALNUM.length())));
        return sb.toString();
    }

    private void safeAudit(String actor, String action, Payment p) {
        safe(() -> ledger.audit(new AuditRequest(actor, action,
                p.getTxnId() + " | " + p.getMode() + " | INR " + p.getAmount() + " | " + p.getFromAccount() + " -> " + p.getToAccount()
                        + (p.getFailureReason() == null ? "" : " | " + p.getFailureReason()))));
    }

    private void safe(Runnable r) {
        try { r.run(); } catch (Exception e) { log.warn("Side-effect call failed: {}", e.getMessage()); }
    }
}
