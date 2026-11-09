package com.example.dgkl.gateway.filter;

import com.example.dgkl.common.security.InternalServiceToken;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class GatewayManagementFilter implements WebFilter {
    private final InternalServiceToken internalToken;

    public GatewayManagementFilter(InternalServiceToken internalToken) {
        this.internalToken = internalToken;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = GatewayRequestPath.matchingPath(exchange);
        if (!path.equals("/actuator") && !path.startsWith("/actuator/")) {
            return chain.filter(exchange);
        }
        if (HttpMethod.GET.equals(exchange.getRequest().getMethod())
                && (path.equals("/actuator/health") || (isReadableManagementEndpoint(path)
                && internalToken.matches(exchange.getRequest().getHeaders().getFirst(
                        InternalServiceToken.HEADER_NAME))))) {
            return chain.filter(exchange);
        }
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        return exchange.getResponse().setComplete();
    }

    private boolean isReadableManagementEndpoint(String path) {
        return path.equals("/actuator") || path.equals("/actuator/") || path.equals("/actuator/info")
                || path.equals("/actuator/metrics") || path.startsWith("/actuator/metrics/");
    }
}
