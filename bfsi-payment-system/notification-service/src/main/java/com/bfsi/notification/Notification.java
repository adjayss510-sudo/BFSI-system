package com.bfsi.notification;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "notifications")
@Getter @Setter
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(nullable = false, length = 20)
    private String channel = "IN_APP";
    @Column(nullable = false, length = 500)
    private String message;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
