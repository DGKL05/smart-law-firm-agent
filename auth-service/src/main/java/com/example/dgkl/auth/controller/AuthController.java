package com.example.dgkl.auth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.dgkl.auth.entity.SysRole;
import com.example.dgkl.auth.entity.SysUser;
import com.example.dgkl.auth.mapper.SysRoleMapper;
import com.example.dgkl.auth.mapper.SysUserMapper;
import com.example.dgkl.auth.mapper.SysUserRoleMapper;
import com.example.dgkl.common.exception.BusinessException;
import com.example.dgkl.common.response.ApiResponse;
import com.example.dgkl.common.security.JwtTokenProvider;
import com.example.dgkl.common.security.SecurityUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        SysUser sysUser = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername())
                .eq(SysUser::getDeleted, 0));
        if (sysUser == null || !matches(request.getPassword(), sysUser.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (sysUser.getStatus() != null && sysUser.getStatus() != 1) {
            throw new BusinessException(403, "账号已停用");
        }
        List<Long> roleIds = sysUserRoleMapper.selectList(new LambdaQueryWrapper<com.example.dgkl.auth.entity.SysUserRole>()
                        .eq(com.example.dgkl.auth.entity.SysUserRole::getUserId, sysUser.getId()))
                .stream()
                .map(com.example.dgkl.auth.entity.SysUserRole::getRoleId)
                .toList();
        List<String> roles = roleIds.isEmpty()
                ? List.of("USER")
                : sysRoleMapper.selectBatchIds(roleIds).stream().map(SysRole::getRoleCode).filter(Objects::nonNull).toList();
        SecurityUser user = new SecurityUser(sysUser.getId(), sysUser.getUsername(), "", roles, true);
        return ApiResponse.success(Map.of(
                "token", jwtTokenProvider.generateToken(user),
                "username", sysUser.getUsername(),
                "nickname", sysUser.getNickname() == null ? sysUser.getUsername() : sysUser.getNickname(),
                "roles", roles
        ));
    }

    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody LoginRequest request) {
        if (sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.getUsername())) > 0) {
            throw new BusinessException("用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getUsername());
        user.setStatus(1);
        user.setDeleted(0);
        sysUserMapper.insert(user);
        return ApiResponse.success();
    }

    @GetMapping("/me")
    public ApiResponse<String> me() {
        return ApiResponse.success("ok");
    }

    private boolean matches(String raw, String encoded) {
        return passwordEncoder.matches(raw, encoded) || raw.equals(encoded);
    }

    @Data
    public static class LoginRequest {
        @NotBlank
        private String username;
        @NotBlank
        private String password;
    }
}
