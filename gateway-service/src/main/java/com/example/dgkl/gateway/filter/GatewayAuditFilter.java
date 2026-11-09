package com.example.dgkl.gateway.filter;

import com.example.dgkl.common.security.InternalServiceToken;
import com.example.dgkl.common.security.SecurityUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class GatewayAuditFilter implements GlobalFilter, Ordered {
    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayAuditFilter.class);
    private static final Set<HttpMethod> WRITE_METHODS =
            Set.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE);
    private static final Duration AUDIT_TIMEOUT = Duration.ofSeconds(2);
    private static final Pattern TARGET_ID = Pattern.compile("[0-9]{1,19}");
    private static final int MAX_TARGET_TYPE_LENGTH = 64;
    private static final int MAX_DETAIL_LENGTH = 2000;
    private final WebClient auditClient;
    private final InternalServiceToken internalToken;

    public GatewayAuditFilter(@Qualifier("internalWebClientBuilder") WebClient.Builder builder,
                              InternalServiceToken internalToken) {
        this.auditClient = builder.baseUrl("http://admin-service").build();
        this.internalToken = internalToken;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = GatewayRequestPath.matchingPath(exchange);
        HttpMethod method = exchange.getRequest().getMethod();
        SecurityUser user = exchange.getAttribute(GatewayAuthFilter.AUTHENTICATED_USER);
        if (user == null || !user.getRoles().contains("ADMIN") || !path.startsWith("/api/admin/")
                || !WRITE_METHODS.contains(method)) {
            return chain.filter(exchange);
        }
        return chain.filter(exchange).then(Mono.defer(() -> recordIfSuccessful(exchange, user, path, method)));
    }

    private Mono<Void> recordIfSuccessful(ServerWebExchange exchange, SecurityUser user, String path,
                                          HttpMethod method) {
        HttpStatusCode status = exchange.getResponse().getStatusCode();
        if (status == null || !status.is2xxSuccessful()) {
            return Mono.empty();
        }
        String[] segments = path.split("/");
        if (segments.length < 4 || segments[3].length() > MAX_TARGET_TYPE_LENGTH
                || path.length() > MAX_DETAIL_LENGTH) {
            return Mono.empty();
        }
        String targetId = segments.length > 4 && TARGET_ID.matcher(segments[4]).matches() ? segments[4] : null;
        AuditRequest record = new AuditRequest(user.getUserId(), method.name(), segments[3], targetId, path);
        return auditClient.post().uri("/internal/admin/audit-events")
                .header(InternalServiceToken.HEADER_NAME, internalToken.value())
                .bodyValue(record).retrieve().toBodilessEntity().timeout(AUDIT_TIMEOUT).then()
                .onErrorResume(exception -> {
                    LOGGER.warn("管理操作审计写入失败，操作员={}，资源={}，错误={}",
                            user.getUserId(), segments[3], exception.getClass().getSimpleName());
                    return Mono.empty();
                });
    }

    @Override
    public int getOrder() {
        return -90;
    }

    private record AuditRequest(Long adminUserId, String action, String targetType, String targetId, String detail) {
    }
}
