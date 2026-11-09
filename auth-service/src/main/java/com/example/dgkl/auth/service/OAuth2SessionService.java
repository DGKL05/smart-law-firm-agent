package com.example.dgkl.auth.service;

import com.example.dgkl.auth.config.OAuth2PersistenceConfig;
import com.example.dgkl.common.security.AccessTokenVerifier;
import com.example.dgkl.common.security.IntrospectionUnavailableException;
import com.example.dgkl.common.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Primary
@Service
@RequiredArgsConstructor
public class OAuth2SessionService implements AccessTokenVerifier {
    private final OAuth2AuthorizationService authorizations;
    private final UserService users;

    @Override
    public Optional<SecurityUser> verify(String token) {
        try {
            OAuth2Authorization authorization = findByAccessToken(token);
            if (authorization == null || authorization.getAccessToken() == null
                    || !authorization.getAccessToken().isActive()) {
                return Optional.empty();
            }
            var claims = authorization.getAccessToken().getClaims();
            return claims == null ? Optional.empty()
                    : currentPrincipal(claims.get(OAuth2PersistenceConfig.USER_ID_CLAIM));
        } catch (DataAccessException exception) {
            log.warn("读取授权状态失败，异常类型 {}", exception.getClass().getSimpleName());
            throw new IntrospectionUnavailableException();
        }
    }

    public Optional<SecurityUser> currentPrincipal(Object userIdClaim) {
        if (!(userIdClaim instanceof String value)) {
            return Optional.empty();
        }
        try {
            long userId = Long.parseLong(value);
            return userId > 0 ? users.findActivePrincipal(userId) : Optional.empty();
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    public void revoke(String token) {
        OAuth2Authorization authorization = findByAccessToken(token);
        if (authorization != null) {
            authorizations.remove(authorization);
        }
    }

    private OAuth2Authorization findByAccessToken(String token) {
        OAuth2Authorization authorization = authorizations.findByToken(token, OAuth2TokenType.ACCESS_TOKEN);
        if (authorization == null
                || !OAuth2PersistenceConfig.SPA_REGISTRATION_ID.equals(authorization.getRegisteredClientId())
                || !AuthorizationGrantType.AUTHORIZATION_CODE.equals(authorization.getAuthorizationGrantType())) {
            return null;
        }
        return authorization;
    }
}
