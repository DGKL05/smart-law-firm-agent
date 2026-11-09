package com.example.dgkl.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class InternalServiceToken {
    public static final String HEADER_NAME = "X-Internal-Token";
    private final String token;

    public InternalServiceToken(
            @Value("${INTERNAL_SERVICE_TOKEN:smart-law-local-internal-token}") String token) {
        if (token.isBlank()) {
            throw new IllegalArgumentException("Internal service token must not be blank");
        }
        this.token = token;
    }

    public String value() {
        return token;
    }

    public boolean matches(String suppliedToken) {
        return suppliedToken != null && MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),
                suppliedToken.getBytes(StandardCharsets.UTF_8));
    }
}
