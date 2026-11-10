package com.bfsi.ledger;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;

/** Append-only: there is intentionally no update/delete API for ledger rows. */
@Entity
@Table(name = "ledger_entries", indexes = {
        @Index(name = "idx_ledger_user", columnList = "user_id"),
        @Index(name = "idx_ledger_txn", columnList = "txn_id")})
@Getter @Setter
public class LedgerEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "txn_id", nullable = false, length = 50)
    private String txnId;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber;
    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 10)
    private EntryType entryType;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @Column(name = "balance_after", precision = 19, scale = 2)
    private BigDecimal balanceAfter;
    @Column(length = 10)
    private String mode;
    @Column(length = 255)
    private String description;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
