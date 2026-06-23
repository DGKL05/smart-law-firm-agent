package com.example.dgkl.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class DifyConfig {
    @Bean
    public RestTemplate difyRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    public Clock aiClock() {
        return Clock.system(ZoneId.of("Asia/Shanghai"));
    }
}
