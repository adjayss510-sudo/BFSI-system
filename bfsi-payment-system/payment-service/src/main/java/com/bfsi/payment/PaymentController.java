package com.bfsi.payment;

import com.bfsi.payment.Dtos.PaymentRequest;
import com.bfsi.payment.Enums.PaymentStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService service;
    private final PaymentRepository repo;

    private void requireAdmin(String role) {
        if (!"ADMIN".equals(role)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
    }

    /** Returns 200 with status SUCCESS | FAILED | PENDING. Send an Idempotency-Key header to make retries safe. */
    @PostMapping
    public Payment pay(@RequestHeader("X-User-Id") Long uid,
                       @RequestHeader("X-User-Email") String email,
                       @RequestHeader(value = "Idempotency-Key", required = false) String idemKey,
                       @Valid @RequestBody PaymentRequest r) {
        return service.process(uid, email, idemKey, r);
    }

    @GetMapping("/me")
    public List<Payment> mine(@RequestHeader("X-User-Id") Long uid) {
        return repo.findByUserIdOrderByIdDesc(uid);
    }

    @GetMapping
    public List<Payment> all(@RequestHeader("X-User-Role") String role) {
        requireAdmin(role);
        return repo.findAllByOrderByIdDesc();
    }

    @GetMapping("/stats")
    public Map<String, Object> stats(@RequestHeader("X-User-Role") String role) {
        requireAdmin(role);
        List<Payment> all = repo.findAll();
        BigDecimal volume = all.stream().filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                .map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalTransactions", all.size());
        m.put("successful", all.stream().filter(p -> p.getStatus() == PaymentStatus.SUCCESS).count());
        m.put("failed", all.stream().filter(p -> p.getStatus() == PaymentStatus.FAILED).count());
        m.put("pending", all.stream().filter(p -> p.getStatus() == PaymentStatus.PENDING).count());
        m.put("successVolume", volume);
        m.put("byMode", all.stream().collect(Collectors.groupingBy(p -> p.getMode().name(), TreeMap::new, Collectors.counting())));
        return m;
    }

    @GetMapping("/{txnId}")
    public Payment one(@RequestHeader("X-User-Id") Long uid, @RequestHeader("X-User-Role") String role, @PathVariable String txnId) {
        Payment p = repo.findByTxnId(txnId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
        if (!"ADMIN".equals(role) && !p.getUserId().equals(uid)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found");
        return p;
    }
}
