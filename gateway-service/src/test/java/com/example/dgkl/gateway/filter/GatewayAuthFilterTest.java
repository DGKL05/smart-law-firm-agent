package com.example.dgkl.gateway.filter;

import com.example.dgkl.common.security.IntrospectionUnavailableException;
import com.example.dgkl.common.security.SecurityUser;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GatewayAuthFilterTest {
    private final GatewayTokenIntrospector introspector = mock(GatewayTokenIntrospector.class);
    private final GatewayAuthFilter filter = new GatewayAuthFilter(introspector);

    @Test
    void rejectsInactiveBearerBeforeCallingBusinessService() {
        when(introspector.verify("inactive-token")).thenReturn(Mono.empty());
        MockServerWebExchange exchange = request("/api/me/appointments", "inactive-token");

        assertThat(forwards(exchange)).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsOrdinaryUserFromAdminRoute() {
        String token = mockToken("user");
        MockServerWebExchange exchange = request("/api/admin/stats", token);

        assertThat(forwards(exchange)).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void appliesAdminPermissionsToEncodedPathSegments() {
        String token = mockToken("user");
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                .method(HttpMethod.GET, URI.create("/api/%61dmin/stats")).header("Authorization", "Bearer " + token));

        assertThat(forwards(exchange)).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void forwardsActiveAdminAndExposesIdentityToAuditFilter() {
        String token = "opaque-admin-token";
        when(introspector.verify(token)).thenReturn(
                Mono.just(new SecurityUser(1L, "admin", "", List.of("ADMIN"), true)));
        MockServerWebExchange exchange = request("/api/admin/stats", token);

        assertThat(forwards(exchange)).isTrue();
        SecurityUser user = exchange.getAttribute(GatewayAuthFilter.AUTHENTICATED_USER);
        assertThat(user).isNotNull();
        assertThat(user.getUserId()).isEqualTo(1L);
    }

    @Test
    void allowsAnonymousPublicImageRead() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/public/media/image"));

        assertThat(forwards(exchange)).isTrue();
    }

    @Test
    void letsTheAuthorizationServerStartAnAnonymousCodeFlow() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/oauth2/authorize"));

        assertThat(forwards(exchange)).isTrue();
    }

    @Test
    void failsClosedWhenIntrospectionIsUnavailable() {
        when(introspector.verify("opaque-token")).thenReturn(Mono.error(new IntrospectionUnavailableException()));
        MockServerWebExchange exchange = request("/api/me/appointments", "opaque-token");

        assertThat(forwards(exchange)).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    private String mockToken(String username) {
        String token = "opaque-user-token";
        when(introspector.verify(token)).thenReturn(
                Mono.just(new SecurityUser(2L, username, "", List.of("USER"), true)));
        return token;
    }

    private MockServerWebExchange request(String path, String token) {
        return MockServerWebExchange.from(
                MockServerHttpRequest.get(path).header("Authorization", "Bearer " + token));
    }

    private boolean forwards(MockServerWebExchange exchange) {
        AtomicBoolean forwarded = new AtomicBoolean();
        filter.filter(exchange, forwardedExchange -> {
            forwarded.set(true);
            return Mono.empty();
        }).block(Duration.ofSeconds(2));
        return forwarded.get();
    }
}
