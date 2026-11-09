package com.example.dgkl.notification.service;

import com.example.dgkl.common.response.PageResult;
import com.example.dgkl.notification.dto.NotificationDeliveryRequest;
import com.example.dgkl.notification.dto.NotificationRequest;
import com.example.dgkl.notification.entity.Notification;

public interface NotificationService {
    PageResult<Notification> list(int pageNum, int pageSize, String keyword, Long userId, Integer readFlag);

    PageResult<Notification> listMine(Long userId, int pageNum, int pageSize, String keyword, Integer readFlag);

    Notification get(Long id);

    Notification getMine(Long userId, Long id);

    Notification create(NotificationRequest request);

    Notification update(Long id, NotificationRequest request);

    void delete(Long id);

    void deleteMine(Long userId, Long id);

    void markRead(Long userId, Long id);

    void deliver(NotificationDeliveryRequest request);

    long count();
}
