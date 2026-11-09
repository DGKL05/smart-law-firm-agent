package com.example.dgkl.lawyer.client;

import com.example.dgkl.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service")
public interface AuthUserClient {
    @GetMapping("/internal/users/{id}/exists")
    ApiResponse<Boolean> exists(@PathVariable("id") Long id);
}
