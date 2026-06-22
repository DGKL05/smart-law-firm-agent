package com.example.dgkl.module.notification.controller;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.dgkl.module.common.AbstractAdminCrudController;
import com.example.dgkl.module.notification.entity.Notification;
import com.example.dgkl.module.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController extends AbstractAdminCrudController<Notification> {
    private final NotificationService notificationService;

    @Override
    protected IService<Notification> service() {
        return notificationService;
    }
}
