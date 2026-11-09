package com.example.dgkl.gateway.filter;

import com.example.dgkl.common.security.BearerToken;
import com.example.dgkl.common.security.IntrospectionUnavailableException;
import com.example.dgkl.common.security.SecurityUser;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;

@Component
public class GatewayAuthFilter implements GlobalFilter, Ordered {
    static final String AUTHENTICATED_USER = GatewayAuthFilter.class.getName() + ".user";
    private static final Set<String> PUBLIC_GET_PATHS = Set.of("/api/oauth2/authorize", "/api/oauth2/csrf",
            "/.well-known/oauth-authorization-server");
    private static final Set<String> PUBLIC_POST_PATHS = Set.of("/api/auth/register", "/api/oauth2/login",
            "/api/oauth2/token");
    private final GatewayTokenIntrospector introspector;

    public GatewayAuthFilter(GatewayTokenIntrospector introspector) {
        this.introspector = introspector;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = GatewayRequestPath.matchingPath(exchange);
        HttpMethod method = exchange.getRequest().getMethod();
        if ((HttpMethod.GET.equals(method) && (path.startsWith("/api/public/") || PUBLIC_GET_PATHS.contains(path)))
                || (HttpMethod.POST.equals(method) && PUBLIC_POST_PATHS.contains(path))) {
            return chain.filter(exchange);
        }
        Optional<String> token = BearerToken.read(
                exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        if (token.isEmpty()) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, "请登录后重试");
        }
        return introspector.verify(token.get()).map(Optional::of).defaultIfEmpty(Optional.empty())
                .flatMap(result -> result.map(user -> authorize(exchange, chain, path, user))
                        .orElseGet(() -> reject(exchange, HttpStatus.UNAUTHORIZED, "登录凭证无效或已过期")))
                .onErrorResume(IntrospectionUnavailableException.class,
                        exception -> reject(exchange, HttpStatus.SERVICE_UNAVAILABLE, "认证服务暂不可用"));
    }

    private Mono<Void> authorize(ServerWebExchange exchange, GatewayFilterChain chain, String path, SecurityUser user) {
        if (path.startsWith("/api/admin/") && !user.getRoles().contains("ADMIN")) {
            return reject(exchange, HttpStatus.FORBIDDEN, "此操作需要管理员权限");
        }
        if ((path.startsWith("/api/lawyer/") || path.startsWith("/api/lawyer-center/"))
                && !user.getRoles().contains("LAWYER")) {
            return reject(exchange, HttpStatus.FORBIDDEN, "此操作需要律师权限");
        }
        exchange.getAttributes().put(AUTHENTICATED_USER, user);
        return chain.filter(exchange);
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        if (HttpStatus.UNAUTHORIZED.equals(status)) {
            exchange.getResponse().getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        }
        String body = "{\"code\":%d,\"message\":\"%s\",\"data\":null}".formatted(status.value(), message);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8))));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
