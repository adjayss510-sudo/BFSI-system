package com.bfsi.auth;

import com.bfsi.auth.Dtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserRepository users;
    private final JwtService jwt;
    private final CustomerClient customers;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /** Onboarding: create the login, then the customer profile + savings account in customer-service. */
    @PostMapping("/register")
    @Transactional
    public AuthResponse register(@Valid @RequestBody RegisterRequest r) {
        String email = r.email().trim().toLowerCase();
        if (users.findByEmail(email).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(r.password()));
        u.setRole("CUSTOMER");
        u = users.saveAndFlush(u);
        try {
            customers.create(new CreateCustomerRequest(u.getId(), r.fullName().trim(), email, r.phone()));
        } catch (Exception e) {
            // RuntimeException -> the user insert is rolled back, so we never leave a login without a profile
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Customer service unavailable; registration rolled back");
        }
        return new AuthResponse(jwt.generate(u), u.getId(), u.getEmail(), u.getRole());
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest r) {
        User u = users.findByEmail(r.email().trim().toLowerCase())
                .filter(x -> encoder.matches(r.password(), x.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        return new AuthResponse(jwt.generate(u), u.getId(), u.getEmail(), u.getRole());
    }
}
