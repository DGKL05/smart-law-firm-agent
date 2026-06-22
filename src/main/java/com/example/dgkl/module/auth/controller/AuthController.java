package com.example.dgkl.module.auth.controller;

import com.example.dgkl.common.BusinessException;
import com.example.dgkl.common.Result;
import com.example.dgkl.module.auth.dto.LoginRequest;
import com.example.dgkl.module.auth.dto.RegisterRequest;
import com.example.dgkl.module.auth.vo.LoginResponse;
import com.example.dgkl.module.user.entity.SysUser;
import com.example.dgkl.module.user.service.SysUserService;
import com.example.dgkl.security.JwtTokenProvider;
import com.example.dgkl.security.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final SysUserService userService;

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        if (userService.findByUsername(request.getUsername()) != null) {
            throw new BusinessException("用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() == null ? request.getUsername() : request.getNickname());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        userService.save(user);
        userService.bindRole(user.getId(), "USER");
        return Result.success();
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        String token = jwtTokenProvider.generateToken(loginUser);
        SysUser user = loginUser.getUser();
        return Result.success(new LoginResponse(token, user.getId(), user.getUsername(), user.getNickname(), loginUser.getRoles()));
    }

    @GetMapping("/me")
    public Result<LoginResponse> me() {
        LoginUser loginUser = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        SysUser user = loginUser.getUser();
        return Result.success(new LoginResponse(null, user.getId(), user.getUsername(), user.getNickname(), loginUser.getRoles()));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }
}
