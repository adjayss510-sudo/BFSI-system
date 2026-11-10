package com.bfsi.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/** Seeds a demo admin: admin@bfsi.com / Admin@123 (change in real environments). */
@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {
    private final UserRepository users;

    @Override
    public void run(String... args) {
        if (users.findByEmail("admin@bfsi.com").isPresent()) return;
        User u = new User();
        u.setEmail("admin@bfsi.com");
        u.setPasswordHash(new BCryptPasswordEncoder().encode("Admin@123"));
        u.setRole("ADMIN");
        users.save(u);
    }
}
