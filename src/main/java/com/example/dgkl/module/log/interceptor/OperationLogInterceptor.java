package com.example.dgkl.module.log.interceptor;

import com.example.dgkl.module.log.document.OperationLogDocument;
import com.example.dgkl.module.log.service.OperationLogService;
import com.example.dgkl.security.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class OperationLogInterceptor implements HandlerInterceptor {
    private final OperationLogService operationLogService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute("logStartAt", System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (!request.getRequestURI().startsWith("/api/") || request.getRequestURI().startsWith("/api/auth/")) {
            return;
        }
        OperationLogDocument document = new OperationLogDocument();
        document.setRequestUri(request.getRequestURI());
        document.setRequestMethod(request.getMethod());
        document.setRequestParams(request.getQueryString());
        document.setIp(request.getRemoteAddr());
        document.setModule(request.getRequestURI().split("/").length > 2 ? request.getRequestURI().split("/")[2] : "api");
        document.setOperation(request.getMethod() + " " + request.getRequestURI());
        document.setSuccess(ex == null && response.getStatus() < 500);
        document.setErrorMessage(ex == null ? null : ex.getMessage());
        document.setCreateTime(LocalDateTime.now());
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            document.setUserId(loginUser.getUserId());
            document.setUsername(loginUser.getUsername());
        }
        operationLogService.saveQuietly(document);
    }
}
