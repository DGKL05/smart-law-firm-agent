package com.example.dgkl.common.config;

import com.example.dgkl.common.response.ApiResponse;
import com.example.dgkl.common.security.AccessTokenVerifier;
import com.example.dgkl.common.security.BearerAuthenticationFilter;
import com.example.dgkl.common.security.InternalServiceToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
@RequiredArgsConstructor
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class ServiceSecurityConfig {
    private final AccessTokenVerifier tokenVerifier;
    private final InternalServiceToken internalServiceToken;
    private final ObjectMapper objectMapper;

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/api/public/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator", "/actuator/info", "/actuator/metrics/**")
                        .access((authentication, context) -> new AuthorizationDecision(internalServiceToken.matches(
                                context.getRequest().getHeader(InternalServiceToken.HEADER_NAME))))
                        .requestMatchers("/internal/**").access((authentication, context) ->
                                new AuthorizationDecision(internalServiceToken.matches(
                                        context.getRequest().getHeader(InternalServiceToken.HEADER_NAME))))
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/lawyer/**", "/api/lawyer-center/**").hasRole("LAWYER")
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) ->
                                writeError(response, HttpStatus.UNAUTHORIZED, "请登录后重试"))
                        .accessDeniedHandler((request, response, exception) ->
                                writeError(response, HttpStatus.FORBIDDEN, "没有权限访问该资源")))
                .addFilterBefore(new BearerAuthenticationFilter(tokenVerifier, objectMapper),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        if (status == HttpStatus.UNAUTHORIZED) {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        }
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ApiResponse.fail(status.value(), message));
    }
}
