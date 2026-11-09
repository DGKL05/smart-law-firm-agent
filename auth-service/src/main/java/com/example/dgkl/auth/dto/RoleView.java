package com.example.dgkl.auth.dto;

import com.example.dgkl.auth.entity.SysRole;

public record RoleView(Long id, String roleCode, String roleName, boolean builtIn) {
    public static RoleView of(SysRole role, boolean builtIn) {
        return new RoleView(role.getId(), role.getRoleCode(), role.getRoleName(), builtIn);
    }
}
