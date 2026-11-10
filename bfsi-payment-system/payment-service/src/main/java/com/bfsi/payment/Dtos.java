package com.bfsi.payment;

import com.bfsi.payment.Enums.PaymentMode;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public final class Dtos {
    private Dtos() {}

    /** toAccount = beneficiary account no (NEFT), VPA or account no (UPI), merchant name (CARD). */
    public record PaymentRequest(@NotBlank String fromAccount,
                                 @NotBlank @Size(max = 100) String toAccount,
                                 @NotNull @DecimalMin(value = "1.00", message = "minimum amount is 1.00") @Digits(integer = 12, fraction = 2) BigDecimal amount,
                                 @NotNull PaymentMode mode,
                                 String ifsc, String cardLast4, @Size(max = 255) String remarks) {}

    // --- Feign payloads ---
    public record TransferRequest(Long userId, String fromAccount, String toAccount, BigDecimal amount) {}
    public record TransferResult(String status, String reason, BigDecimal fromBalanceAfter, BigDecimal toBalanceAfter, Long toUserId) {}
    public record LedgerRequest(String txnId, Long userId, String accountNumber, String entryType,
                                BigDecimal amount, BigDecimal balanceAfter, String mode, String description) {}
    public record AuditRequest(String actor, String action, String details) {}
    public record NotifyRequest(Long userId, String message) {}
}
