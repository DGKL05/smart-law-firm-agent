package com.example.dgkl.module.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dgkl.common.PageResult;
import com.example.dgkl.common.Result;
import com.example.dgkl.module.common.EntityLifecycle;
import com.example.dgkl.module.user.entity.SysUser;
import com.example.dgkl.module.user.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {
    private final SysUserService userService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public Result<PageResult<SysUser>> page(@RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "10") long pageSize,
                                            @RequestParam(required = false) String keyword) {
        QueryWrapper<SysUser> wrapper = new QueryWrapper<SysUser>().orderByDesc("create_time");
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like("username", keyword).or().like("nickname", keyword);
        }
        return Result.success(PageResult.of(userService.page(new Page<>(pageNum, pageSize), wrapper)));
    }

    @PostMapping
    public Result<SysUser> create(@RequestBody SysUser user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userService.save(user);
        userService.bindRole(user.getId(), "USER");
        return Result.success(user);
    }

    @PutMapping("/{id}")
    public Result<SysUser> update(@PathVariable Long id, @RequestBody SysUser user) {
        user.setId(id);
        if (user.getPassword() != null && !user.getPassword().startsWith("$2")) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        EntityLifecycle.forUpdate(user);
        userService.updateById(user);
        return Result.success(userService.getById(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.removeById(id);
        return Result.success();
    }
}
