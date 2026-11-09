package com.example.dgkl.gateway.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration(proxyBeanMethods = false)
public class GatewayClientConfig {
    @Bean
    @LoadBalanced
    public WebClient.Builder internalWebClientBuilder(ObjectProvider<WebClientCustomizer> customizers) {
        return observedBuilder(customizers);
    }

    @Bean
    @Primary
    public WebClient.Builder oauthWebClientBuilder(ObjectProvider<WebClientCustomizer> customizers) {
        return observedBuilder(customizers);
    }

    private WebClient.Builder observedBuilder(ObjectProvider<WebClientCustomizer> customizers) {
        WebClient.Builder builder = WebClient.builder();
        customizers.orderedStream().forEach(customizer -> customizer.customize(builder));
        return builder;
    }
}
