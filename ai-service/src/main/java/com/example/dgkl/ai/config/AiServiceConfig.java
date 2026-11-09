package com.example.dgkl.ai.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class AiServiceConfig {
    @Bean
    public TransactionTemplate aiChatTransactionTemplate(PlatformTransactionManager manager) {
        TransactionTemplate transactions = new TransactionTemplate(manager);
        transactions.setTimeout(10);
        return transactions;
    }

    @Bean
    public RestTemplate difyRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(90))
                .build();
    }
}
