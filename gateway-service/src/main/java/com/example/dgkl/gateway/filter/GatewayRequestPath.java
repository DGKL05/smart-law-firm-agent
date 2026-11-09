package com.example.dgkl.gateway.filter;

import org.springframework.http.server.PathContainer;
import org.springframework.web.server.ServerWebExchange;

import java.util.stream.Collectors;

final class GatewayRequestPath {
    private GatewayRequestPath() {
    }

    static String matchingPath(ServerWebExchange exchange) {
        return exchange.getRequest().getPath().pathWithinApplication().elements().stream()
                .map(element -> element instanceof PathContainer.PathSegment segment
                        ? segment.valueToMatch() : element.value())
                .collect(Collectors.joining());
    }
}
