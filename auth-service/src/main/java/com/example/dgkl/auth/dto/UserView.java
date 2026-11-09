package com.example.dgkl.auth.dto;

import com.example.dgkl.auth.entity.SysUser;

import java.util.List;

public record UserView(
        Long id, String username, String nickname, String phone, String email, List<String> roles, Integer status) {
    public static UserView of(SysUser user, List<String> roles) {
        return new UserView(user.getId(), user.getUsername(), user.getNickname(),
                user.getPhone(), user.getEmail(), roles, user.getStatus());
    }
}
