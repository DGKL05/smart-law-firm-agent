package com.example.dgkl.auth.controller;

import com.example.dgkl.auth.dto.AuthRequest;
import com.example.dgkl.auth.dto.UserView;
import com.example.dgkl.auth.service.OAuth2SessionService;
import com.example.dgkl.auth.service.UserService;
import com.example.dgkl.common.response.ApiResponse;
import com.example.dgkl.common.security.BearerToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    private final OAuth2SessionService sessions;

    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody AuthRequest.Credentials request) {
        userService.register(request);
        return ApiResponse.success();
    }

    @GetMapping("/me")
    public ApiResponse<UserView> me() {
        return ApiResponse.success(userService.currentProfile());
    }

    @PutMapping("/me")
    public ApiResponse<UserView> updateMe(@Valid @RequestBody AuthRequest.Profile request) {
        return ApiResponse.success(userService.updateProfile(request));
    }

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody AuthRequest.Password password,
                                            HttpServletRequest request) {
        userService.changePassword(password);
        invalidateLoginSession(request);
        return ApiResponse.success();
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        BearerToken.read(request.getHeader(HttpHeaders.AUTHORIZATION)).ifPresent(sessions::revoke);
        invalidateLoginSession(request);
        return ApiResponse.success();
    }

    private void invalidateLoginSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
