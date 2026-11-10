package com.bfsi.customer;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class Dtos {
    private Dtos() {}

    public record CreateCustomerRequest(Long userId, String fullName, String email, String phone) {}

    public record KycRequest(@NotBlank String fullName,
                             @NotNull @Past LocalDate dob,
                             @NotBlank @Pattern(regexp = "[A-Z]{5}[0-9]{4}[A-Z]", message = "invalid PAN format (e.g. ABCDE1234F)") String pan,
                             @NotBlank @Pattern(regexp = "\\d{12}", message = "Aadhaar must be 12 digits") String aadhaar,
                             @NotBlank @Size(max = 500) String address) {}

    public record CustomerView(Long id, Long userId, String fullName, String email, String phone, LocalDate dob,
                               String panMasked, String aadhaarMasked, String address, KycStatus kycStatus, Instant createdAt) {
        public static CustomerView from(Customer c) {
            String pan = c.getPan() == null ? null : "XXXXXX" + c.getPan().substring(6);
            String aad = c.getAadhaarLast4() == null ? null : "XXXX-XXXX-" + c.getAadhaarLast4();
            return new CustomerView(c.getId(), c.getUserId(), c.getFullName(), c.getEmail(), c.getPhone(), c.getDob(),
                    pan, aad, c.getAddress(), c.getKycStatus(), c.getCreatedAt());
        }
    }

    public record TransferRequest(Long userId, String fromAccount, String toAccount, BigDecimal amount) {}

    /** status = SUCCESS | FAILED. toUserId/toBalanceAfter are set only when the beneficiary is an internal account. */
    public record TransferResult(String status, String reason, BigDecimal fromBalanceAfter,
                                 BigDecimal toBalanceAfter, Long toUserId) {
        public static TransferResult fail(String reason) { return new TransferResult("FAILED", reason, null, null, null); }
    }
}
