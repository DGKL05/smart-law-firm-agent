package com.example.dgkl.common.security;

import com.example.dgkl.common.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
public class BearerAuthenticationFilter extends OncePerRequestFilter {
    private final AccessTokenVerifier verifier;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.equals("/actuator") || path.startsWith("/actuator/") || path.startsWith("/internal/")
                || ("GET".equals(request.getMethod()) && path.startsWith("/api/public/"))
                || ("POST".equals(request.getMethod()) && path.equals("/api/auth/register"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            BearerToken.read(request.getHeader(HttpHeaders.AUTHORIZATION)).flatMap(verifier::verify).ifPresent(user -> {
                var authentication = UsernamePasswordAuthenticationToken.authenticated(
                        user, null, user.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            });
        } catch (IntrospectionUnavailableException exception) {
            int status = HttpStatus.SERVICE_UNAVAILABLE.value();
            response.setStatus(status);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            objectMapper.writeValue(response.getWriter(), ApiResponse.fail(status, "认证服务暂不可用"));
            return;
        }
        chain.doFilter(request, response);
    }
}
