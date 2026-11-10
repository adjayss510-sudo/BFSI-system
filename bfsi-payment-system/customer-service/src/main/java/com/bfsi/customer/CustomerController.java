package com.bfsi.customer;

import com.bfsi.customer.Dtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {
    private final CustomerRepository customers;
    private final AccountRepository accounts;

    private Customer find(Long userId) {
        return customers.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer profile not found"));
    }

    private void requireAdmin(String role) {
        if (!"ADMIN".equals(role)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
    }

    @GetMapping("/me")
    public CustomerView me(@RequestHeader("X-User-Id") Long uid) {
        return CustomerView.from(find(uid));
    }

    @PutMapping("/me/kyc")
    public CustomerView submitKyc(@RequestHeader("X-User-Id") Long uid, @Valid @RequestBody KycRequest r) {
        Customer c = find(uid);
        if (c.getKycStatus() == KycStatus.VERIFIED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "KYC already verified");
        c.setFullName(r.fullName().trim());
        c.setDob(r.dob());
        c.setPan(r.pan());
        c.setAadhaarLast4(r.aadhaar().substring(8));
        c.setAddress(r.address().trim());
        c.setKycStatus(KycStatus.PENDING);
        c.setUpdatedAt(Instant.now());
        return CustomerView.from(customers.save(c));
    }

    @GetMapping("/me/accounts")
    public List<Account> myAccounts(@RequestHeader("X-User-Id") Long uid) {
        return accounts.findByUserId(uid);
    }

    // ---- Admin ----
    @GetMapping
    public List<CustomerView> all(@RequestHeader("X-User-Role") String role) {
        requireAdmin(role);
        return customers.findAll().stream().map(CustomerView::from).toList();
    }

    @GetMapping("/accounts")
    public List<Account> allAccounts(@RequestHeader("X-User-Role") String role) {
        requireAdmin(role);
        return accounts.findAll();
    }

    @PutMapping("/{id}/kyc-status")
    public CustomerView setKyc(@RequestHeader("X-User-Role") String role, @PathVariable Long id, @RequestParam KycStatus status) {
        requireAdmin(role);
        if (status != KycStatus.VERIFIED && status != KycStatus.REJECTED)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status must be VERIFIED or REJECTED");
        Customer c = customers.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
        if (c.getKycStatus() == KycStatus.NOT_SUBMITTED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Customer has not submitted KYC");
        c.setKycStatus(status);
        c.setUpdatedAt(Instant.now());
        return CustomerView.from(customers.save(c));
    }
}
