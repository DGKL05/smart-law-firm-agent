package com.example.dgkl.notification.service;

import com.example.dgkl.common.response.PageResult;
import com.example.dgkl.notification.dto.NotificationTemplateRequest;
import com.example.dgkl.notification.entity.NotificationTemplate;

public interface NotificationTemplateService {
    PageResult<NotificationTemplate> list(int pageNum, int pageSize, String keyword);

    NotificationTemplate get(Long id);

    NotificationTemplate create(NotificationTemplateRequest request);

    NotificationTemplate update(Long id, NotificationTemplateRequest request);

    void delete(Long id);
}
