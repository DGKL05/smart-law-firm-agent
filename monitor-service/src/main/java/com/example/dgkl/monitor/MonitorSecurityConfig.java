package com.example.dgkl.monitor;

import de.codecentric.boot.admin.server.web.client.HttpHeadersProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
public class MonitorSecurityConfig {
    @Bean
    public SecurityFilterChain monitorSecurity(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(requests -> requests
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .csrf(csrf -> csrf.ignoringRequestMatchers("/instances", "/instances/**"))
                .build();
    }

    @Bean
    public HttpHeadersProvider managementHeaders(@Value("${INTERNAL_SERVICE_TOKEN}") String token) {
        return instance -> {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Token", token);
            return headers;
        };
    }
}
