package com.example.dgkl.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dgkl.auth.dto.RoleRequest;
import com.example.dgkl.auth.dto.RoleView;
import com.example.dgkl.auth.entity.SysRole;
import com.example.dgkl.auth.entity.SysUserRole;
import com.example.dgkl.auth.mapper.SysRoleMapper;
import com.example.dgkl.auth.mapper.SysUserRoleMapper;
import com.example.dgkl.auth.service.RoleService;
import com.example.dgkl.auth.service.UserService;
import com.example.dgkl.common.exception.BusinessException;
import com.example.dgkl.common.response.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {
    private static final Set<String> BUILT_IN_ROLES = Set.of("ADMIN", "USER", "LAWYER");

    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final UserService userService;

    @Override
    public PageResult<RoleView> listRoles(long pageNum, long pageSize, String keyword) {
        userService.requireAdministrator();
        LambdaQueryWrapper<SysRole> query = new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId);
        if (StringUtils.hasText(keyword)) {
            String text = keyword.strip();
            query.and(group -> group.like(SysRole::getRoleCode, text).or().like(SysRole::getRoleName, text));
        }
        Page<SysRole> page = roleMapper.selectPage(Page.of(pageNum, pageSize), query);
        List<RoleView> records = page.getRecords().stream().map(this::view).toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public RoleView getRole(Long id) {
        userService.requireAdministrator();
        return view(requireRole(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoleView createRole(RoleRequest request) {
        lockAdministratorWrites();
        SysRole role = new SysRole();
        role.setRoleCode(request.roleCode());
        role.setRoleName(request.roleName().strip());
        role.setDeleted(0);
        try {
            roleMapper.insert(role);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(409, "角色编码已存在");
        }
        return view(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoleView updateRole(Long id, RoleRequest request) {
        lockAdministratorWrites();
        SysRole role = requireRole(id);
        if (BUILT_IN_ROLES.contains(role.getRoleCode()) && !role.getRoleCode().equals(request.roleCode())) {
            throw new BusinessException(409, "不能修改系统内置角色的编码");
        }
        role.setRoleCode(request.roleCode());
        role.setRoleName(request.roleName().strip());
        try {
            roleMapper.updateById(role);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(409, "角色编码已存在");
        }
        return view(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long id) {
        lockAdministratorWrites();
        SysRole role = requireRole(id);
        if (BUILT_IN_ROLES.contains(role.getRoleCode())) {
            throw new BusinessException(409, "不能删除系统内置角色");
        }
        if (userRoleMapper.exists(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, id))) {
            throw new BusinessException(409, "角色正在使用，请先移除用户的角色关联");
        }
        roleMapper.deleteById(id);
    }

    private SysRole requireRole(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(404, "角色不存在");
        }
        return role;
    }

    private void lockAdministratorWrites() {
        if (roleMapper.lockAdministratorRole() == null) {
            throw new BusinessException(503, "系统管理员角色尚未初始化");
        }
        userService.requireAdministrator();
    }

    private RoleView view(SysRole role) {
        return RoleView.of(role, BUILT_IN_ROLES.contains(role.getRoleCode()));
    }
}
