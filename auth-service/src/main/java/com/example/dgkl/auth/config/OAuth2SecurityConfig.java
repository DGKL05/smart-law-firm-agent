package com.example.dgkl.auth.config;

import com.example.dgkl.auth.service.OAuth2SessionService;
import com.example.dgkl.auth.service.UserService;
import com.example.dgkl.common.security.SecurityUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenIntrospection;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.ClientSecretAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.authentication
        .OAuth2TokenIntrospectionAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration
        .OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers
        .OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.http.converter
        .OAuth2TokenIntrospectionHttpMessageConverter;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenClaimsContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;

@Configuration(proxyBeanMethods = false)
public class OAuth2SecurityConfig {
    private static final String AUTHORIZE_ENDPOINT = "/api/oauth2/authorize";
    private static final String LOGIN_ENDPOINT = "/api/oauth2/login";
    private static final String LOGIN_PAGE = "/login?oauth=1";
    private final OAuth2TokenIntrospectionHttpMessageConverter introspectionWriter =
            new OAuth2TokenIntrospectionHttpMessageConverter();

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerChain(
            HttpSecurity http, HttpSessionRequestCache oauthRequestCache, OAuth2SessionService sessions)
            throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .clientAuthentication(clients -> clients.authenticationProviders(this::useClientSecretEncoder))
                .tokenIntrospectionEndpoint(endpoint -> endpoint.introspectionResponseHandler(
                        (request, response, authentication) -> writeIntrospection(response, authentication, sessions)));
        return http.requestCache(cache -> cache.requestCache(oauthRequestCache))
                .exceptionHandling(errors -> errors.defaultAuthenticationEntryPointFor(
                        (request, response, exception) -> redirect(response, LOGIN_PAGE),
                        new AntPathRequestMatcher(AUTHORIZE_ENDPOINT)))
                .build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain authorizationLoginChain(
            HttpSecurity http, HttpSessionRequestCache oauthRequestCache, DaoAuthenticationProvider formProvider)
            throws Exception {
        return http.securityMatcher(LOGIN_ENDPOINT, "/api/oauth2/csrf")
                .authenticationProvider(formProvider)
                .requestCache(cache -> cache.requestCache(oauthRequestCache))
                .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
                .formLogin(form -> form.loginPage("/login").loginProcessingUrl(LOGIN_ENDPOINT)
                        .successHandler((request, response, authentication) ->
                                resumeAuthorization(request, response, oauthRequestCache))
                        .failureHandler((request, response, exception) -> redirect(response, LOGIN_PAGE + "&error=1")))
                .build();
    }

    @Bean
    public HttpSessionRequestCache oauthRequestCache() {
        var cache = new HttpSessionRequestCache();
        cache.setRequestMatcher(new AntPathRequestMatcher(AUTHORIZE_ENDPOINT, "GET"));
        cache.setMatchingRequestParameterName(null);
        return cache;
    }

    @Bean
    public DaoAuthenticationProvider formProvider(UserService users, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(users::loadForAuthentication);
        provider.setPasswordEncoder(encoder);
        return provider;
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings(
            @Value("${OAUTH2_ISSUER:http://localhost:5173}") String issuer) {
        return AuthorizationServerSettings.builder()
                .issuer(issuer)
                .authorizationEndpoint(AUTHORIZE_ENDPOINT)
                .tokenEndpoint("/api/oauth2/token")
                .tokenIntrospectionEndpoint("/internal/oauth2/introspect")
                .tokenRevocationEndpoint("/internal/oauth2/revoke")
                .build();
    }

    @Bean
    public OAuth2TokenCustomizer<OAuth2TokenClaimsContext> opaqueTokenClaims(UserService users) {
        return context -> {
            if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())
                    && AuthorizationGrantType.AUTHORIZATION_CODE.equals(context.getAuthorizationGrantType())
                    && OAuth2PersistenceConfig.SPA_REGISTRATION_ID.equals(context.getRegisteredClient().getId())) {
                // 登录主体名是用户表主键，账号改名后令牌也会绑定同一个人。
                long userId = Long.parseLong(context.getPrincipal().getName());
                if (users.findActivePrincipal(userId).isEmpty()) {
                    throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_GRANT);
                }
                // Persist a string supported by the authorization service's default JSON allowlist.
                context.getClaims().claim(OAuth2PersistenceConfig.USER_ID_CLAIM, Long.toString(userId));
            }
        };
    }

    private void useClientSecretEncoder(List<AuthenticationProvider> providers) {
        PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        for (AuthenticationProvider provider : providers) {
            if (provider instanceof ClientSecretAuthenticationProvider clientSecrets) {
                clientSecrets.setPasswordEncoder(encoder);
            }
        }
    }

    private void writeIntrospection(HttpServletResponse response, Authentication authentication,
                                    OAuth2SessionService sessions) throws IOException {
        var result = (OAuth2TokenIntrospectionAuthenticationToken) authentication;
        OAuth2TokenIntrospection original = result.getTokenClaims();
        Optional<SecurityUser> user = original.isActive()
                ? sessions.currentPrincipal(original.getClaims().get(OAuth2PersistenceConfig.USER_ID_CLAIM))
                : Optional.empty();
        OAuth2TokenIntrospection current = user.map(account ->
                OAuth2TokenIntrospection.withClaims(original.getClaims())
                        .username(account.getUsername())
                        .claim(OAuth2PersistenceConfig.USER_ID_CLAIM, account.getUserId())
                        .claim("roles", account.getRoles())
                        .build()).orElseGet(() -> OAuth2TokenIntrospection.builder(false).build());
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        introspectionWriter.write(current, MediaType.APPLICATION_JSON, new ServletServerHttpResponse(response));
    }

    private void resumeAuthorization(HttpServletRequest request, HttpServletResponse response,
                                     HttpSessionRequestCache cache) {
        SavedRequest saved = cache.getRequest(request, response);
        cache.removeRequest(request, response);
        if (saved == null) {
            redirect(response, "/login");
            return;
        }
        URI redirect = URI.create(saved.getRedirectUrl());
        if (!AUTHORIZE_ENDPOINT.equals(redirect.getRawPath())) {
            redirect(response, "/login");
            return;
        }
        // 只恢复授权请求的路径和查询参数，浏览器保持当前访问的前端域名和端口。
        String query = redirect.getRawQuery();
        redirect(response, AUTHORIZE_ENDPOINT + (query == null ? "" : "?" + query));
    }

    private void redirect(HttpServletResponse response, String location) {
        response.setStatus(HttpStatus.FOUND.value());
        response.setHeader(HttpHeaders.LOCATION, location);
    }
}
