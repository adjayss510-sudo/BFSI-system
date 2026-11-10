package com.bfsi.auth;

import jakarta.validation.constraints.*;

public final class Dtos {
    private Dtos() {}
    public record RegisterRequest(@NotBlank String fullName,
                                  @NotBlank @Email String email,
                                  @NotBlank @Pattern(regexp = "\\d{10}", message = "phone must be 10 digits") String phone,
                                  @NotBlank @Size(min = 8, message = "password must be at least 8 characters") String password) {}
    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}
    public record AuthResponse(String token, Long userId, String email, String role) {}
    public record CreateCustomerRequest(Long userId, String fullName, String email, String phone) {}
}
