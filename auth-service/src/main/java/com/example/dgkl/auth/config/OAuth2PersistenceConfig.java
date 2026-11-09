package com.example.dgkl.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration(proxyBeanMethods = false)
public class OAuth2PersistenceConfig {
    public static final String SPA_REGISTRATION_ID = "smart-law-spa-v1";
    public static final String USER_ID_CLAIM = "user_id";
    private static final String SPA_CLIENT_ID = "smart-law-spa";
    private static final String RESOURCE_SERVER_REGISTRATION_ID = "smart-law-resource-server-v1";
    private static final String API_SCOPE = "smart-law-api";
    private static final String DEFAULT_CALLBACKS =
            "http://localhost:5173/oauth/callback,http://127.0.0.1:5173/oauth/callback"
                    + ",http://localhost:5174/oauth/callback,http://127.0.0.1:5174/oauth/callback";

    @Bean
    public RegisteredClientRepository registeredClientRepository(
            JdbcTemplate jdbc,
            @Value("${OAUTH2_REDIRECT_URIS:" + DEFAULT_CALLBACKS + "}") String redirectUris,
            @Value("${OAUTH2_INTROSPECTION_CLIENT_ID:smart-law-resource-server}") String resourceServerClientId,
            @Value("${OAUTH2_INTROSPECTION_CLIENT_SECRET}") String resourceServerSecret) {
        var clients = new JdbcRegisteredClientRepository(jdbc);
        Set<String> callbacks = Arrays.stream(redirectUris.split(",")).map(String::strip)
                .filter(uri -> !uri.isEmpty()).collect(Collectors.toUnmodifiableSet());
        if (callbacks.isEmpty()) {
            throw new IllegalArgumentException("At least one OAuth2 redirect URI is required");
        }
        clients.save(spaClient(clients, callbacks));
        clients.save(resourceServerClient(clients, resourceServerClientId, resourceServerSecret));
        return clients;
    }

    @Bean
    public OAuth2AuthorizationService authorizationService(JdbcTemplate jdbc, RegisteredClientRepository clients) {
        return new JdbcOAuth2AuthorizationService(jdbc, clients);
    }

    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService(
            JdbcTemplate jdbc, RegisteredClientRepository clients) {
        return new JdbcOAuth2AuthorizationConsentService(jdbc, clients);
    }

    private RegisteredClient spaClient(RegisteredClientRepository clients, Set<String> callbacks) {
        RegisteredClient stored = clients.findById(SPA_REGISTRATION_ID);
        RegisteredClient.Builder builder = stored == null
                ? RegisteredClient.withId(SPA_REGISTRATION_ID) : RegisteredClient.from(stored);
        return builder.clientId(SPA_CLIENT_ID)
                .clientName("Smart Law Browser")
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUris(uris -> {
                    uris.clear();
                    uris.addAll(callbacks);
                })
                .scope(API_SCOPE)
                .clientSettings(ClientSettings.builder().requireProofKey(true).build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenFormat(OAuth2TokenFormat.REFERENCE)
                        .authorizationCodeTimeToLive(Duration.ofMinutes(3))
                        .accessTokenTimeToLive(Duration.ofHours(1))
                        .build())
                .build();
    }

    private RegisteredClient resourceServerClient(
            RegisteredClientRepository clients, String clientId, String secret) {
        int secretLength = secret.getBytes(StandardCharsets.UTF_8).length;
        if (secretLength < 32 || secretLength > 72) {
            throw new IllegalArgumentException("The OAuth2 introspection secret must be 32 to 72 bytes");
        }
        RegisteredClient stored = clients.findById(RESOURCE_SERVER_REGISTRATION_ID);
        if (stored != null && !stored.getClientId().equals(clientId)) {
            throw new IllegalArgumentException("The registered OAuth2 introspection client ID has changed");
        }
        PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        String encodedSecret = stored != null && encoder.matches(secret, stored.getClientSecret())
                ? stored.getClientSecret() : encoder.encode(secret);
        RegisteredClient.Builder builder = stored == null
                ? RegisteredClient.withId(RESOURCE_SERVER_REGISTRATION_ID) : RegisteredClient.from(stored);
        return builder.clientId(clientId)
                .clientSecret(encodedSecret)
                .clientName("Smart Law Resource Services")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("token:introspect")
                .tokenSettings(TokenSettings.builder().accessTokenFormat(OAuth2TokenFormat.REFERENCE).build())
                .build();
    }
}
