package com.example.dgkl.module.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.dgkl.common.PageResult;
import com.example.dgkl.module.user.entity.SysUser;

import java.util.List;

public interface SysUserService extends IService<SysUser> {
    SysUser findByUsername(String username);

    List<String> findRoleCodes(Long userId);

    void bindRole(Long userId, String roleCode);

    PageResult<SysUser> adminPage(long pageNum, long pageSize, String keyword);

    SysUser adminCreate(SysUser user);

    SysUser adminUpdate(Long id, SysUser user);

    void adminDelete(Long id);
}
