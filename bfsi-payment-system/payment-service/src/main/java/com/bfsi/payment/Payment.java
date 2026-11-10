package com.bfsi.payment;

import com.bfsi.payment.Enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments",
        uniqueConstraints = @UniqueConstraint(name = "uk_payment_idem", columnNames = {"user_id", "idempotency_key"}),
        indexes = @Index(name = "idx_payment_user", columnList = "user_id"))
@Getter @Setter
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "txn_id", nullable = false, unique = true, length = 50)
    private String txnId;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "user_email", length = 150)
    private String userEmail;
    @Column(name = "from_account", nullable = false, length = 20)
    private String fromAccount;
    @Column(name = "to_account", nullable = false, length = 100)   // account no / VPA / merchant
    private String toAccount;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PaymentMode mode;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PaymentStatus status;
    @Column(name = "failure_reason", length = 255)
    private String failureReason;
    @Column(length = 11)
    private String ifsc;
    @Column(name = "card_last4", length = 4)
    private String cardLast4;
    @Column(length = 255)
    private String remarks;
    @Column(name = "idempotency_key", length = 64)
    private String idempotencyKey;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
