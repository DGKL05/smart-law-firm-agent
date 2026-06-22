package com.example.dgkl.module.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.dgkl.module.user.entity.SysUser;

import java.util.List;

public interface SysUserService extends IService<SysUser> {
    SysUser findByUsername(String username);

    List<String> findRoleCodes(Long userId);

    void bindRole(Long userId, String roleCode);
}
