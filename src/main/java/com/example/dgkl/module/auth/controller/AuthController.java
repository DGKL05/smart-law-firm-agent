package com.example.dgkl.module.auth.controller;

import com.example.dgkl.common.Result;
import com.example.dgkl.module.auth.dto.LoginRequest;
import com.example.dgkl.module.auth.dto.RegisterRequest;
import com.example.dgkl.module.auth.service.AuthService;
import com.example.dgkl.module.auth.vo.LoginResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return Result.success();
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @GetMapping("/me")
    public Result<LoginResponse> me() {
        return Result.success(authService.currentUser());
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }
}
