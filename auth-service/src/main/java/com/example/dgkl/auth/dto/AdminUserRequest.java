package com.example.dgkl.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AdminUserRequest(
        @NotBlank @Size(max = 64) String username,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) @Size(max = 72) String password,
        @Size(max = 64) String nickname,
        @Size(max = 32) String phone,
        @Email @Size(max = 128) String email,
        @Min(0) @Max(1) Integer status,
        @Size(min = 1, max = 20) List<@NotBlank @Pattern(regexp = "[A-Z][A-Z0-9_]{0,63}") String> roleCodes) {
}
