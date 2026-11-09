package com.example.dgkl.gateway.filter;

import com.example.dgkl.common.security.IntrospectionUnavailableException;
import com.example.dgkl.common.security.OAuth2IntrospectionResponse;
import com.example.dgkl.common.security.OAuth2IntrospectionSettings;
import com.example.dgkl.common.security.SecurityUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class GatewayTokenIntrospector {
    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayTokenIntrospector.class);
    private final WebClient client;
    private final OAuth2IntrospectionSettings settings;

    public GatewayTokenIntrospector(OAuth2IntrospectionSettings settings,
                                    @Qualifier("oauthWebClientBuilder") WebClient.Builder builder) {
        this.settings = settings;
        this.client = builder.defaultHeaders(settings::authorize).build();
    }

    public Mono<SecurityUser> verify(String token) {
        return client.post().uri(settings.uri())
                .body(BodyInserters.fromFormData("token", token).with("token_type_hint", "access_token"))
                .retrieve().bodyToMono(OAuth2IntrospectionResponse.class)
                .switchIfEmpty(Mono.error(IntrospectionUnavailableException::new))
                .timeout(Duration.ofSeconds(3))
                .onErrorMap(exception -> {
                    LOGGER.warn("网关令牌内省失败，异常类型 {}", exception.getClass().getSimpleName());
                    return new IntrospectionUnavailableException();
                })
                .flatMap(response -> Mono.justOrEmpty(response.principal()));
    }
}
