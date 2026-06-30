package com.example.dgkl.module.notification.controller;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.dgkl.common.Result;
import com.example.dgkl.module.common.AbstractUserCrudController;
import com.example.dgkl.module.notification.entity.Notification;
import com.example.dgkl.module.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/notifications")
@RequiredArgsConstructor
public class MyNotificationController extends AbstractUserCrudController<Notification> {
    private final NotificationService notificationService;

    @Override
    protected IService<Notification> service() {
        return notificationService;
    }

    @PutMapping("/{id}/read")
    public Result<Notification> read(@PathVariable Long id) {
        return Result.success(notificationService.readCurrentUserNotification(id));
    }
}
