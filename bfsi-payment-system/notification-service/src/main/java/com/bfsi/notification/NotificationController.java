package com.bfsi.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Stores in-app alerts and logs them. Swap the log line for an SMS/e-mail gateway in production. */
@Slf4j
@RestController
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationRepository repo;

    public record NotifyRequest(Long userId, String message) {}

    @PostMapping("/internal/notify")
    public void receive(@RequestBody NotifyRequest r) {
        Notification n = new Notification();
        n.setUserId(r.userId());
        n.setMessage(r.message());
        repo.save(n);
        log.info("[ALERT user={}] {}", r.userId(), r.message());
    }

    @GetMapping("/notifications/me")
    public List<Notification> mine(@RequestHeader("X-User-Id") Long uid) {
        return repo.findByUserIdOrderByIdDesc(uid);
    }

    @GetMapping("/notifications")
    public List<Notification> all(@RequestHeader("X-User-Role") String role) {
        if (!"ADMIN".equals(role)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        return repo.findAllByOrderByIdDesc();
    }
}
