package com.example.dgkl.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dgkl.auth.config.OAuth2PersistenceConfig;
import com.example.dgkl.auth.dto.AdminUserRequest;
import com.example.dgkl.auth.dto.AuthRequest;
import com.example.dgkl.auth.dto.UserView;
import com.example.dgkl.auth.entity.SysRole;
import com.example.dgkl.auth.entity.SysUser;
import com.example.dgkl.auth.entity.SysUserRole;
import com.example.dgkl.auth.mapper.SysRoleMapper;
import com.example.dgkl.auth.mapper.SysUserMapper;
import com.example.dgkl.auth.mapper.SysUserRoleMapper;
import com.example.dgkl.auth.service.UserService;
import com.example.dgkl.common.exception.BusinessException;
import com.example.dgkl.common.response.PageResult;
import com.example.dgkl.common.security.SecurityUser;
import com.example.dgkl.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String USER_ROLE = "USER";
    private static final int ACTIVE = 1;
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int MAX_BCRYPT_BYTES = 72;

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadForAuthentication(String username) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("Unknown login account");
        }
        return User.withUsername(user.getId().toString())
                .password(user.getPassword())
                .roles(USER_ROLE)
                .disabled(!isActive(user.getStatus()))
                .build();
    }

    @Override
    public Optional<SecurityUser> findActivePrincipal(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null || !isActive(user.getStatus())) {
            return Optional.empty();
        }
        return Optional.of(new SecurityUser(user.getId(), user.getUsername(), "", roleCodes(user.getId()), true));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(AuthRequest.Credentials request) {
        String password = encodeNewPassword(request.password());
        SysRole role = defaultRole();
        SysUser user = new SysUser();
        user.setUsername(request.username());
        user.setPassword(password);
        user.setNickname(request.username());
        user.setStatus(ACTIVE);
        user.setDeleted(0);
        insertUser(user);
        addRoles(user.getId(), List.of(role));
    }

    @Override
    public UserView currentProfile() {
        SysUser user = requireActiveUser();
        return UserView.of(user, roleCodes(user.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserView updateProfile(AuthRequest.Profile request) {
        SysUser user = requireActiveUser();
        String nickname = optionalText(request.nickname());
        String phone = optionalText(request.phone());
        String email = optionalText(request.email());
        int rows = userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, user.getId())
                .eq(SysUser::getStatus, ACTIVE)
                .set(SysUser::getNickname, nickname)
                .set(SysUser::getPhone, phone)
                .set(SysUser::getEmail, email));
        if (rows == 0) {
            throw new BusinessException(409, "账号状态已变化，请重新登录");
        }
        user.setNickname(nickname);
        user.setPhone(phone);
        user.setEmail(email);
        return UserView.of(user, roleCodes(user.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(AuthRequest.Password request) {
        SysUser user = requireActiveUser();
        if (!matchesPassword(request.oldPassword(), user.getPassword())) {
            throw new BusinessException(400, "原密码不正确");
        }
        int rows = userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, user.getId())
                .eq(SysUser::getStatus, ACTIVE)
                .eq(SysUser::getPassword, user.getPassword())
                .set(SysUser::getPassword, encodeNewPassword(request.newPassword())));
        if (rows == 0) {
            throw new BusinessException(409, "密码或账号状态已变化，请刷新后重试");
        }
        revokeAuthorizations(user.getId());
    }

    @Override
    public PageResult<UserView> listUsers(long pageNum, long pageSize, String keyword) {
        requireAdministrator();
        LambdaQueryWrapper<SysUser> query = new LambdaQueryWrapper<SysUser>().orderByDesc(SysUser::getId);
        if (StringUtils.hasText(keyword)) {
            String text = keyword.strip();
            query.and(group -> group.like(SysUser::getUsername, text)
                    .or().like(SysUser::getNickname, text)
                    .or().like(SysUser::getEmail, text)
                    .or().like(SysUser::getPhone, text));
        }
        Page<SysUser> page = userMapper.selectPage(Page.of(pageNum, pageSize), query);
        List<Long> userIds = page.getRecords().stream().map(SysUser::getId).toList();
        Map<Long, List<String>> roles = roleCodesByUser(userIds);
        List<UserView> records = page.getRecords().stream()
                .map(user -> UserView.of(user, roles.getOrDefault(user.getId(), List.of(USER_ROLE))))
                .toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public UserView getUser(Long id) {
        requireAdministrator();
        return UserView.of(requireUser(id), roleCodes(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserView createUser(AdminUserRequest request) {
        String password = encodeNewPassword(request.password());
        lockAdministratorWrites();
        requireAdministrator();
        List<SysRole> roles = request.roleCodes() == null
                ? List.of(defaultRole()) : requestedRoles(request.roleCodes());
        SysUser user = new SysUser();
        user.setUsername(request.username());
        user.setPassword(password);
        user.setNickname(optionalText(request.nickname()));
        user.setPhone(optionalText(request.phone()));
        user.setEmail(optionalText(request.email()));
        user.setStatus(request.status() == null ? ACTIVE : request.status());
        user.setDeleted(0);
        insertUser(user);
        addRoles(user.getId(), roles);
        return UserView.of(user, codesOf(roles));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserView updateUser(Long id, AdminUserRequest request) {
        String password = StringUtils.hasText(request.password()) ? encodeNewPassword(request.password()) : null;
        lockAdministratorWrites();
        requireAdministrator();
        SysUser user = requireUser(id);
        List<String> previousRoles = roleCodes(id);
        List<SysRole> newRoles = request.roleCodes() == null ? null : requestedRoles(request.roleCodes());
        List<String> newRoleCodes = newRoles == null ? previousRoles : codesOf(newRoles);
        Integer status = request.status() == null ? user.getStatus() : request.status();
        protectLastAdministrator(user, previousRoles, isActive(status) && newRoleCodes.contains(ADMIN_ROLE));

        LambdaUpdateWrapper<SysUser> changes = new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, id)
                .set(SysUser::getUsername, request.username())
                .set(SysUser::getNickname, optionalText(request.nickname()))
                .set(SysUser::getPhone, optionalText(request.phone()))
                .set(SysUser::getEmail, optionalText(request.email()))
                .set(SysUser::getStatus, status)
                .set(password != null, SysUser::getPassword, password);
        try {
            userMapper.update(null, changes);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(409, "用户名已存在");
        }
        if (newRoles != null) {
            removeRoles(id);
            addRoles(id, newRoles);
        }
        if (!isActive(status) || password != null) {
            revokeAuthorizations(id);
        }
        user.setUsername(request.username());
        user.setNickname(optionalText(request.nickname()));
        user.setPhone(optionalText(request.phone()));
        user.setEmail(optionalText(request.email()));
        user.setStatus(status);
        return UserView.of(user, newRoleCodes);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        lockAdministratorWrites();
        Long operatorId = requireAdministrator();
        if (operatorId.equals(id)) {
            throw new BusinessException(409, "不能删除当前管理员账号");
        }
        SysUser user = requireUser(id);
        protectLastAdministrator(user, roleCodes(id), false);
        userMapper.deleteById(id);
        removeRoles(id);
        revokeAuthorizations(id);
    }

    @Override
    public boolean activeUserExists(Long id) {
        return userMapper.exists(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getId, id)
                .eq(SysUser::getStatus, ACTIVE));
    }

    @Override
    public long countUsers() {
        return userMapper.selectCount(null);
    }

    @Override
    public Long requireAdministrator() {
        Long userId = requireUserId();
        if (!userMapper.isActiveAdministrator(userId)) {
            throw new BusinessException(403, "仅启用的管理员账号可以执行此操作");
        }
        return userId;
    }

    private void lockAdministratorWrites() {
        // 以系统管理员角色行串行化用户角色、状态变更，避免并发操作移除所有管理员。
        if (roleMapper.lockAdministratorRole() == null) {
            throw new BusinessException(503, "系统管理员角色尚未初始化");
        }
    }

    private void protectLastAdministrator(SysUser user, List<String> roles, boolean remainsActiveAdministrator) {
        if (isActive(user.getStatus()) && roles.contains(ADMIN_ROLE)
                && !remainsActiveAdministrator && userMapper.countActiveAdministrators() <= 1) {
            throw new BusinessException(409, "必须保留至少一个启用的管理员");
        }
    }

    private SysUser requireActiveUser() {
        SysUser user = userMapper.selectById(requireUserId());
        if (user == null || !isActive(user.getStatus())) {
            throw new BusinessException(403, "账号不存在或已停用");
        }
        return user;
    }

    private Long requireUserId() {
        Long userId = SecurityUtils.currentUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        return userId;
    }

    private SysUser requireUser(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }

    private SysRole defaultRole() {
        SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, USER_ROLE));
        if (role == null) {
            throw new BusinessException(503, "普通用户角色尚未初始化");
        }
        return role;
    }

    private List<SysRole> requestedRoles(List<String> roleCodes) {
        Set<String> codes = new HashSet<>(roleCodes);
        List<SysRole> roles = roleMapper.selectList(new LambdaQueryWrapper<SysRole>().in(SysRole::getRoleCode, codes));
        if (!new HashSet<>(codesOf(roles)).equals(codes)) {
            throw new BusinessException(400, "存在无效的角色");
        }
        return roles;
    }

    private void insertUser(SysUser user) {
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(409, "用户名已存在");
        }
    }

    private void addRoles(Long userId, List<SysRole> roles) {
        for (SysRole role : roles) {
            SysUserRole assignment = new SysUserRole();
            assignment.setUserId(userId);
            assignment.setRoleId(role.getId());
            userRoleMapper.insert(assignment);
        }
    }

    private void removeRoles(Long userId) {
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
    }

    private void revokeAuthorizations(Long userId) {
        userMapper.revokeAuthorizations(OAuth2PersistenceConfig.SPA_REGISTRATION_ID, userId.toString());
    }

    private List<String> roleCodes(Long userId) {
        return roleCodesByUser(List.of(userId)).getOrDefault(userId, List.of(USER_ROLE));
    }

    private Map<Long, List<String>> roleCodesByUser(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        List<SysUserRole> assignments = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                .in(SysUserRole::getUserId, userIds));
        if (assignments.isEmpty()) {
            return Map.of();
        }
        List<Long> roleIds = assignments.stream().map(SysUserRole::getRoleId).distinct().toList();
        Map<Long, SysRole> roles = roleMapper.selectByIds(roleIds).stream()
                .collect(Collectors.toMap(SysRole::getId, Function.identity()));
        Map<Long, List<String>> result = new HashMap<>();
        for (SysUserRole assignment : assignments) {
            SysRole role = roles.get(assignment.getRoleId());
            if (role != null) {
                result.computeIfAbsent(assignment.getUserId(), key -> new ArrayList<>()).add(role.getRoleCode());
            }
        }
        result.replaceAll((key, values) -> values.stream().sorted().toList());
        return result;
    }

    private List<String> codesOf(List<SysRole> roles) {
        return roles.stream().map(SysRole::getRoleCode).sorted().toList();
    }

    private String encodeNewPassword(String password) {
        if (!StringUtils.hasText(password) || password.length() < MIN_PASSWORD_LENGTH
                || password.getBytes(StandardCharsets.UTF_8).length > MAX_BCRYPT_BYTES) {
            throw new BusinessException(400, "密码至少 6 个字符，UTF-8 编码后不能超过 72 字节");
        }
        return passwordEncoder.encode(password);
    }

    private boolean matchesPassword(String raw, String encoded) {
        return raw.getBytes(StandardCharsets.UTF_8).length <= MAX_BCRYPT_BYTES && passwordEncoder.matches(raw, encoded);
    }

    private boolean isActive(Integer status) {
        return Integer.valueOf(ACTIVE).equals(status);
    }

    private String optionalText(String value) {
        return StringUtils.hasText(value) ? value.strip() : null;
    }
}
