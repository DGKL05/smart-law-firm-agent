package com.example.dgkl.notification.controller;

import com.example.dgkl.common.response.ApiResponse;
import com.example.dgkl.common.response.PageResult;
import com.example.dgkl.common.security.SecurityUtils;
import com.example.dgkl.notification.entity.Notification;
import com.example.dgkl.notification.service.NotificationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/notifications")
@RequiredArgsConstructor
public class UserNotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<PageResult<Notification>> list(
            @RequestParam(defaultValue = "1") @Min(1) int pageNum,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize,
            @RequestParam(required = false) @Size(max = 128) String keyword,
            @RequestParam(required = false) @Min(0) @Max(1) Integer readFlag) {
        return ApiResponse.success(notificationService.listMine(
                SecurityUtils.currentUserId(), pageNum, pageSize, keyword, readFlag));
    }

    @GetMapping("/{id}")
    public ApiResponse<Notification> get(@PathVariable @Positive Long id) {
        return ApiResponse.success(notificationService.getMine(SecurityUtils.currentUserId(), id));
    }

    @PutMapping("/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable @Positive Long id) {
        notificationService.markRead(SecurityUtils.currentUserId(), id);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable @Positive Long id) {
        notificationService.deleteMine(SecurityUtils.currentUserId(), id);
        return ApiResponse.success();
    }
}
