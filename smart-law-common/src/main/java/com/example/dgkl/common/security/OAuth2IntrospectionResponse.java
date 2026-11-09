package com.example.dgkl.common.security;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OAuth2IntrospectionResponse(
        boolean active,
        @JsonProperty("user_id") Long userId,
        String username,
        List<String> roles) {
    public Optional<SecurityUser> principal() {
        if (!active || userId == null || userId <= 0 || username == null || username.isBlank()
                || roles == null || roles.isEmpty() || roles.stream().anyMatch(Objects::isNull)) {
            return Optional.empty();
        }
        return Optional.of(new SecurityUser(userId, username, "", List.copyOf(roles), true));
    }
}
