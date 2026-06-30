package com.example.dgkl.module.notification.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.dgkl.module.notification.entity.Notification;

public interface NotificationService extends IService<Notification> {
    Notification readCurrentUserNotification(Long id);
}
