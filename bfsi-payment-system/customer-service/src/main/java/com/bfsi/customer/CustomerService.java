package com.bfsi.customer;

import com.bfsi.customer.Dtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class CustomerService {
    private static final BigDecimal OPENING_BALANCE = new BigDecimal("50000.00"); // demo money
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final CustomerRepository customers;
    private final AccountRepository accounts;

    /** Idempotent: calling twice for the same user does nothing the second time. */
    @Transactional
    public void create(CreateCustomerRequest r) {
        if (customers.findByUserId(r.userId()).isPresent()) return;
        Customer c = new Customer();
        c.setUserId(r.userId());
        c.setFullName(r.fullName());
        c.setEmail(r.email());
        c.setPhone(r.phone());
        customers.save(c);

        Account a = new Account();
        a.setUserId(r.userId());
        String no;
        do { no = String.valueOf(ThreadLocalRandom.current().nextLong(100_000_000_000L, 1_000_000_000_000L)); }
        while (accounts.existsByAccountNumber(no));
        a.setAccountNumber(no);
        a.setBalance(OPENING_BALANCE);
        accounts.save(a);
    }

    /**
     * Atomic money movement: debit source, credit beneficiary if it is one of our accounts.
     * Rows are locked in account-number order to avoid deadlocks between opposite transfers.
     */
    @Transactional
    public TransferResult transfer(TransferRequest r) {
        if (r.amount() == null || r.amount().signum() <= 0) return TransferResult.fail("Invalid amount");
        String a = r.fromAccount(), b = r.toAccount();
        if (a.equals(b)) return TransferResult.fail("Source and beneficiary account are the same");

        Account from, to;
        if (b != null && b.compareTo(a) < 0) {
            to = accounts.lockByNumber(b).orElse(null);
            from = accounts.lockByNumber(a).orElse(null);
        } else {
            from = accounts.lockByNumber(a).orElse(null);
            to = b == null ? null : accounts.lockByNumber(b).orElse(null);
        }

        if (from == null || !from.getUserId().equals(r.userId())) return TransferResult.fail("Invalid source account");
        if (!"ACTIVE".equals(from.getStatus())) return TransferResult.fail("Source account is not active");
        boolean kycOk = customers.findByUserId(r.userId()).map(c -> c.getKycStatus() == KycStatus.VERIFIED).orElse(false);
        if (!kycOk) return TransferResult.fail("KYC not verified. Complete KYC to make payments");
        if (from.getBalance().compareTo(r.amount()) < 0) return TransferResult.fail("Insufficient funds");

        from.setBalance(from.getBalance().subtract(r.amount()));
        if (to != null) to.setBalance(to.getBalance().add(r.amount()));
        return new TransferResult("SUCCESS", null, from.getBalance(),
                to == null ? null : to.getBalance(), to == null ? null : to.getUserId());
    }
}
