package com.example.dgkl.common.security;

import java.util.Optional;
import java.util.regex.Pattern;

public final class BearerToken {
    private static final String PREFIX = "Bearer ";
    private static final int MAX_LENGTH = 2048;
    private static final Pattern TOKEN_VALUE = Pattern.compile("[A-Za-z0-9._~+/-]+=*");

    private BearerToken() {
    }

    public static Optional<String> read(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, PREFIX, 0, PREFIX.length())) {
            return Optional.empty();
        }
        String value = authorization.substring(PREFIX.length());
        return value.length() <= MAX_LENGTH && TOKEN_VALUE.matcher(value).matches()
                ? Optional.of(value) : Optional.empty();
    }
}
