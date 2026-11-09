package com.example.dgkl.auth.controller;

import com.example.dgkl.auth.dto.AdminUserRequest;
import com.example.dgkl.auth.dto.UserView;
import com.example.dgkl.auth.service.UserService;
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
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {
    private final UserService userService;

    @GetMapping
    public ApiResponse<PageResult<UserView>> list(
            @RequestParam(defaultValue = "1") @Min(1) @Max(Integer.MAX_VALUE) long pageNum,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long pageSize,
            @RequestParam(required = false) @Size(max = 128) String keyword) {
        return ApiResponse.success(userService.listUsers(pageNum, pageSize, keyword));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserView> get(@PathVariable @Positive Long id) {
        return ApiResponse.success(userService.getUser(id));
    }

    @PostMapping
    public ApiResponse<UserView> create(@Valid @RequestBody AdminUserRequest request) {
        return ApiResponse.success(userService.createUser(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserView> update(@PathVariable @Positive Long id, @Valid @RequestBody AdminUserRequest request) {
        return ApiResponse.success(userService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable @Positive Long id) {
        userService.deleteUser(id);
        return ApiResponse.success();
    }
}
