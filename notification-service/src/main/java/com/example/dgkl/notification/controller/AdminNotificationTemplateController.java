package com.example.dgkl.notification.controller;

import com.example.dgkl.common.response.ApiResponse;
import com.example.dgkl.common.response.PageResult;
import com.example.dgkl.notification.dto.NotificationTemplateRequest;
import com.example.dgkl.notification.entity.NotificationTemplate;
import com.example.dgkl.notification.service.NotificationTemplateService;
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
@RequestMapping("/api/admin/notification-templates")
@RequiredArgsConstructor
public class AdminNotificationTemplateController {
    private final NotificationTemplateService templateService;

    @GetMapping
    public ApiResponse<PageResult<NotificationTemplate>> list(
            @RequestParam(defaultValue = "1") @Min(1) int pageNum,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize,
            @RequestParam(required = false) @Size(max = 128) String keyword) {
        return ApiResponse.success(templateService.list(pageNum, pageSize, keyword));
    }

    @GetMapping("/{id}")
    public ApiResponse<NotificationTemplate> get(@PathVariable @Positive Long id) {
        return ApiResponse.success(templateService.get(id));
    }

    @PostMapping
    public ApiResponse<NotificationTemplate> create(@Valid @RequestBody NotificationTemplateRequest request) {
        return ApiResponse.success(templateService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<NotificationTemplate> update(@PathVariable @Positive Long id,
                                                    @Valid @RequestBody NotificationTemplateRequest request) {
        return ApiResponse.success(templateService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable @Positive Long id) {
        templateService.delete(id);
        return ApiResponse.success();
    }
}
