package com.example.dgkl.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dgkl.common.BusinessException;
import com.example.dgkl.module.user.entity.SysRole;
import com.example.dgkl.module.user.entity.SysUser;
import com.example.dgkl.module.user.entity.SysUserRole;
import com.example.dgkl.module.user.mapper.SysRoleMapper;
import com.example.dgkl.module.user.mapper.SysUserMapper;
import com.example.dgkl.module.user.mapper.SysUserRoleMapper;
import com.example.dgkl.module.user.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;

    @Override
    public SysUser findByUsername(String username) {
        return getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username).last("limit 1"));
    }

    @Override
    public List<String> findRoleCodes(Long userId) {
        List<SysUserRole> links = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (links.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> roleIds = links.stream().map(SysUserRole::getRoleId).toList();
        return roleMapper.selectBatchIds(roleIds).stream().map(SysRole::getRoleCode).toList();
    }

    @Override
    public void bindRole(Long userId, String roleCode) {
        SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, roleCode).last("limit 1"));
        if (role == null) {
            throw new BusinessException("角色不存在：" + roleCode);
        }
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(role.getId());
        userRoleMapper.insert(userRole);
    }

    @Override
    public boolean save(SysUser entity) {
        LocalDateTime now = LocalDateTime.now();
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        entity.setDeleted(0);
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        return super.save(entity);
    }
}
