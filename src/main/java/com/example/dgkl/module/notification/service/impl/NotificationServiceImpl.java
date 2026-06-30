package com.example.dgkl.module.notification.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dgkl.common.BusinessException;
import com.example.dgkl.module.common.EntityLifecycle;
import com.example.dgkl.module.notification.entity.Notification;
import com.example.dgkl.module.notification.mapper.NotificationMapper;
import com.example.dgkl.module.notification.service.NotificationService;
import com.example.dgkl.security.SecurityUtils;
import org.springframework.stereotype.Service;

@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {
    @Override
    public Notification readCurrentUserNotification(Long id) {
        Notification notification = getById(id);
        Long userId = SecurityUtils.currentUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        if (notification == null || !userId.equals(notification.getUserId())) {
            throw new BusinessException(403, "只能访问自己的数据");
        }
        notification.setReadStatus("已读");
        EntityLifecycle.forUpdate(notification);
        updateById(notification);
        return getById(id);
    }
}
