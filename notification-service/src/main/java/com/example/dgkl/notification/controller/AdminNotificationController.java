package com.example.dgkl.notification.controller;

import com.example.dgkl.common.response.ApiResponse;
import com.example.dgkl.common.response.PageResult;
import com.example.dgkl.notification.dto.NotificationRequest;
import com.example.dgkl.notification.entity.Notification;
import com.example.dgkl.notification.service.NotificationService;
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
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<PageResult<Notification>> list(
            @RequestParam(defaultValue = "1") @Min(1) int pageNum,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize,
            @RequestParam(required = false) @Size(max = 128) String keyword,
            @RequestParam(required = false) @Positive Long userId,
            @RequestParam(required = false) @Min(0) @Max(1) Integer readFlag) {
        return ApiResponse.success(notificationService.list(pageNum, pageSize, keyword, userId, readFlag));
    }

    @GetMapping("/{id}")
    public ApiResponse<Notification> get(@PathVariable @Positive Long id) {
        return ApiResponse.success(notificationService.get(id));
    }

    @PostMapping
    public ApiResponse<Notification> create(@Valid @RequestBody NotificationRequest request) {
        return ApiResponse.success(notificationService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Notification> update(@PathVariable @Positive Long id,
                                            @Valid @RequestBody NotificationRequest request) {
        return ApiResponse.success(notificationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable @Positive Long id) {
        notificationService.delete(id);
        return ApiResponse.success();
    }
}
