package com.example.dgkl.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public final class AuditRequests {
    private AuditRequests() {
    }

    public record Manual(@NotBlank @Size(max = 128) String action,
                         @Size(max = 64) String targetType,
                         @Size(max = 64) String targetId,
                         @Size(max = 2000) String detail) {
    }

    public record Edit(@Size(max = 2000) String detail) {
    }

    public record Gateway(@NotNull @Positive Long adminUserId,
                          @NotBlank @Pattern(regexp = "POST|PUT|PATCH|DELETE") String action,
                          @NotBlank @Size(max = 64) String targetType,
                          @Size(max = 64) String targetId,
                          @NotBlank @Size(max = 2000) String detail) {
    }
}
