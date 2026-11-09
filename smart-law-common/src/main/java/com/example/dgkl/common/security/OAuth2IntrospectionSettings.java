package com.example.dgkl.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2IntrospectionSettings {
    private final URI uri;
    private final String clientId;
    private final String clientSecret;

    public OAuth2IntrospectionSettings(
            @Value("${OAUTH2_INTROSPECTION_URI:http://localhost:8081/internal/oauth2/introspect}") URI uri,
            @Value("${OAUTH2_INTROSPECTION_CLIENT_ID:smart-law-resource-server}") String clientId,
            @Value("${OAUTH2_INTROSPECTION_CLIENT_SECRET}") String clientSecret) {
        if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                || uri.getHost() == null || uri.getUserInfo() != null || clientId.isBlank() || clientSecret.isBlank()) {
            throw new IllegalArgumentException("Invalid OAuth2 introspection settings");
        }
        this.uri = uri;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public URI uri() {
        return uri;
    }

    public void authorize(HttpHeaders headers) {
        headers.setBasicAuth(URLEncoder.encode(clientId, StandardCharsets.UTF_8),
                URLEncoder.encode(clientSecret, StandardCharsets.UTF_8));
    }
}
