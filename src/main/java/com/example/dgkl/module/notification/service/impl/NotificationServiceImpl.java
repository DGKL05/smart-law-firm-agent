package com.example.dgkl.module.notification.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dgkl.module.notification.entity.Notification;
import com.example.dgkl.module.notification.mapper.NotificationMapper;
import com.example.dgkl.module.notification.service.NotificationService;
import org.springframework.stereotype.Service;

@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {
}
