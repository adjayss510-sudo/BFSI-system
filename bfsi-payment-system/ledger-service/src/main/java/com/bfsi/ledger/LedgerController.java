package com.bfsi.ledger;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class LedgerController {
    private final LedgerRepository ledger;
    private final AuditRepository audit;

    public record LedgerRequest(String txnId, Long userId, String accountNumber, EntryType entryType,
                                BigDecimal amount, BigDecimal balanceAfter, String mode, String description) {}
    public record AuditRequest(String actor, String action, String details) {}

    private void requireAdmin(String role) {
        if (!"ADMIN".equals(role)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
    }

    // ---- public (via gateway) ----
    @GetMapping("/ledger/me")
    public List<LedgerEntry> mine(@RequestHeader("X-User-Id") Long uid) {
        return ledger.findByUserIdOrderByIdDesc(uid);
    }

    @GetMapping("/ledger")
    public List<LedgerEntry> all(@RequestHeader("X-User-Role") String role, @RequestParam(required = false) String txnId) {
        requireAdmin(role);
        return txnId == null ? ledger.findAllByOrderByIdDesc() : ledger.findByTxnIdOrderByIdAsc(txnId);
    }

    @GetMapping("/ledger/audit")
    public List<AuditLog> auditTrail(@RequestHeader("X-User-Role") String role) {
        requireAdmin(role);
        return audit.findAllByOrderByIdDesc();
    }

    // ---- internal (service-to-service) ----
    @PostMapping("/internal/ledger")
    public LedgerEntry record(@RequestBody LedgerRequest r) {
        LedgerEntry e = new LedgerEntry();
        e.setTxnId(r.txnId());
        e.setUserId(r.userId());
        e.setAccountNumber(r.accountNumber());
        e.setEntryType(r.entryType());
        e.setAmount(r.amount());
        e.setBalanceAfter(r.balanceAfter());
        e.setMode(r.mode());
        e.setDescription(r.description());
        return ledger.save(e);
    }

    @PostMapping("/internal/audit")
    public void audit(@RequestBody AuditRequest r) {
        AuditLog a = new AuditLog();
        a.setActor(r.actor());
        a.setAction(r.action());
        a.setDetails(r.details());
        audit.save(a);
    }
}
