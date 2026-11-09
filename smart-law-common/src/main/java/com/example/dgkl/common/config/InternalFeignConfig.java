package com.example.dgkl.common.config;

import com.example.dgkl.common.security.InternalServiceToken;
import feign.Logger;
import feign.Request;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(RequestInterceptor.class)
public class InternalFeignConfig {
    @Bean
    public RequestInterceptor internalServiceTokenInterceptor(InternalServiceToken token) {
        return request -> {
            if (request.path().startsWith("/internal/")) {
                request.header(InternalServiceToken.HEADER_NAME, token.value());
            }
        };
    }

    @Bean
    public Request.Options serviceCallOptions(
            @Value("${smart-law.feign.connect-timeout-ms:2000}") long connectTimeout,
            @Value("${smart-law.feign.read-timeout-ms:5000}") long readTimeout) {
        return new Request.Options(connectTimeout, TimeUnit.MILLISECONDS, readTimeout, TimeUnit.MILLISECONDS, false);
    }

    @Bean
    public Logger.Level serviceCallLogLevel() {
        return Logger.Level.BASIC;
    }
}
