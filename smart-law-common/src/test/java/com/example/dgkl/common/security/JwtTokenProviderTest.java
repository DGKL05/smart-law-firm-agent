package com.example.dgkl.common.security;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenProviderTest {
    @Test
    void tokenKeepsUserIdAndRoles() {
        JwtTokenProvider provider = new JwtTokenProvider("smart-law-firm-local-dev-secret-key-change-before-prod-2026", 60000);
        SecurityUser user = new SecurityUser(19L, "lawyer", "secret", List.of("LAWYER"), true);

        String token = provider.generateToken(user);
        SecurityUser parsed = provider.parseUser(token);

        assertEquals(19L, parsed.getUserId());
        assertEquals("lawyer", parsed.getUsername());
        assertTrue(parsed.getRoles().contains("LAWYER"));
    }
}
