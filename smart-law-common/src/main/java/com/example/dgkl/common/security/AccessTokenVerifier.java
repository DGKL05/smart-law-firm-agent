package com.example.dgkl.common.security;

import java.util.Optional;

public interface AccessTokenVerifier {
    Optional<SecurityUser> verify(String token);
}
