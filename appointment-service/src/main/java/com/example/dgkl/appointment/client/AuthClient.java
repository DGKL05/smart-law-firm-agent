package com.example.dgkl.appointment.client;

import com.example.dgkl.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service")
public interface AuthClient {
    @GetMapping("/internal/users/{id}/exists")
    ApiResponse<Boolean> exists(@PathVariable("id") Long id);
}
