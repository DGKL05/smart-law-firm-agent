package com.example.dgkl.notification.controller;

import com.example.dgkl.common.response.ApiResponse;
import com.example.dgkl.notification.dto.NotificationDeliveryRequest;
import com.example.dgkl.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalNotificationController {
    private final NotificationService notificationService;

    @PostMapping("/notifications")
    public ApiResponse<Void> deliver(@Valid @RequestBody NotificationDeliveryRequest request) {
        notificationService.deliver(request);
        return ApiResponse.success();
    }

    @GetMapping("/admin/stats")
    public ApiResponse<Map<String, Long>> stats() {
        return ApiResponse.success(Map.of("notificationCount", notificationService.count()));
    }
}
