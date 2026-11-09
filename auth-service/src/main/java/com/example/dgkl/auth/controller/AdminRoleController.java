package com.example.dgkl.auth.controller;

import com.example.dgkl.auth.dto.RoleRequest;
import com.example.dgkl.auth.dto.RoleView;
import com.example.dgkl.auth.service.RoleService;
import com.example.dgkl.common.response.ApiResponse;
import com.example.dgkl.common.response.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
public class AdminRoleController {
    private final RoleService roleService;

    @GetMapping
    public ApiResponse<PageResult<RoleView>> list(
            @RequestParam(defaultValue = "1") @Min(1) @Max(Integer.MAX_VALUE) long pageNum,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long pageSize,
            @RequestParam(required = false) @Size(max = 64) String keyword) {
        return ApiResponse.success(roleService.listRoles(pageNum, pageSize, keyword));
    }

    @GetMapping("/{id}")
    public ApiResponse<RoleView> get(@PathVariable @Positive Long id) {
        return ApiResponse.success(roleService.getRole(id));
    }

    @PostMapping
    public ApiResponse<RoleView> create(@Valid @RequestBody RoleRequest request) {
        return ApiResponse.success(roleService.createRole(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<RoleView> update(@PathVariable @Positive Long id, @Valid @RequestBody RoleRequest request) {
        return ApiResponse.success(roleService.updateRole(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable @Positive Long id) {
        roleService.deleteRole(id);
        return ApiResponse.success();
    }
}
