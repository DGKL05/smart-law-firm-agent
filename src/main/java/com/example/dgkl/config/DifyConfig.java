package com.example.dgkl.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class DifyConfig {
    @Bean
    public RestTemplate difyRestTemplate() {
        return new RestTemplate();
    }
}
