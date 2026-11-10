package com.bfsi.customer;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "accounts")
@Getter @Setter
public class Account {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "account_number", nullable = false, unique = true, length = 20)
    private String accountNumber;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "account_type", nullable = false, length = 20)
    private String accountType = "SAVINGS";
    @Column(nullable = false, length = 11)
    private String ifsc = "BFSI0000001";
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;
    @Column(nullable = false, length = 10)
    private String status = "ACTIVE";
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
