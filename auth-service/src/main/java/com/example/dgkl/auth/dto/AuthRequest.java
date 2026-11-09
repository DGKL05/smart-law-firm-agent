package com.example.dgkl.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthRequest {
    private AuthRequest() {
    }

    public record Credentials(
            @NotBlank @Size(max = 64) String username,
            @NotBlank @Size(max = 72) String password) {
    }

    public record Profile(
            @Size(max = 64) String nickname,
            @Size(max = 32) String phone,
            @Email @Size(max = 128) String email) {
    }

    public record Password(
            @NotBlank @Size(max = 72) String oldPassword,
            @NotBlank @Size(min = 6, max = 72) String newPassword) {
    }
}
