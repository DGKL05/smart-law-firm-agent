package com.example.dgkl.auth.controller;

import com.example.dgkl.auth.service.UserService;
import com.example.dgkl.common.response.ApiResponse;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class AuthInternalController {
    private final UserService userService;

    @GetMapping("/users/{id}/exists")
    public ApiResponse<Boolean> userExists(@PathVariable @Positive Long id) {
        return ApiResponse.success(userService.activeUserExists(id));
    }

    @GetMapping("/admin/stats")
    public ApiResponse<Map<String, Long>> stats() {
        return ApiResponse.success(Map.of("userCount", userService.countUsers()));
    }
}
