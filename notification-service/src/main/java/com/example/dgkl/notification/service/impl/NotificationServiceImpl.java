package com.example.dgkl.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dgkl.common.exception.BusinessException;
import com.example.dgkl.common.response.ApiResponse;
import com.example.dgkl.common.response.PageResult;
import com.example.dgkl.notification.client.AuthUserClient;
import com.example.dgkl.notification.dto.NotificationDeliveryRequest;
import com.example.dgkl.notification.dto.NotificationRequest;
import com.example.dgkl.notification.entity.Notification;
import com.example.dgkl.notification.mapper.NotificationMapper;
import com.example.dgkl.notification.service.NotificationService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private static final int UNREAD = 0;
    private static final int READ = 1;

    private final NotificationMapper notificationMapper;
    private final AuthUserClient authUserClient;

    @Override
    public PageResult<Notification> list(int pageNum, int pageSize, String keyword, Long userId, Integer readFlag) {
        LambdaQueryWrapper<Notification> query = Wrappers.lambdaQuery(Notification.class)
                .eq(userId != null, Notification::getUserId, userId)
                .eq(readFlag != null, Notification::getReadFlag, readFlag)
                .orderByDesc(Notification::getId);
        if (StringUtils.hasText(keyword)) {
            query.and(condition -> condition.like(Notification::getTitle, keyword)
                    .or().like(Notification::getContent, keyword));
        }
        Page<Notification> page = notificationMapper.selectPage(Page.of(pageNum, pageSize), query);
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public PageResult<Notification> listMine(Long userId, int pageNum, int pageSize, String keyword, Integer readFlag) {
        return list(pageNum, pageSize, keyword, requireCurrentUser(userId), readFlag);
    }

    @Override
    public Notification get(Long id) {
        return requireNotification(notificationMapper.selectById(id));
    }

    @Override
    public Notification getMine(Long userId, Long id) {
        return requireNotification(notificationMapper.selectOne(ownNotification(userId, id)));
    }

    @Override
    @Transactional
    public Notification create(NotificationRequest request) {
        validateRecipient(request.userId());
        Notification notification = new Notification();
        notification.setUserId(request.userId());
        notification.setTitle(request.title().strip());
        notification.setContent(request.content());
        notification.setReadFlag(Objects.requireNonNullElse(request.readFlag(), UNREAD));
        notificationMapper.insert(notification);
        return get(notification.getId());
    }

    @Override
    @Transactional
    public Notification update(Long id, NotificationRequest request) {
        Notification notification = requireNotification(notificationMapper.selectOne(Wrappers
                .lambdaQuery(Notification.class).eq(Notification::getId, id).last("FOR UPDATE")));
        if (!Objects.equals(notification.getUserId(), request.userId())) {
            validateRecipient(request.userId());
        }
        notificationMapper.update(null, Wrappers.lambdaUpdate(Notification.class)
                .eq(Notification::getId, id)
                .set(Notification::getUserId, request.userId())
                .set(Notification::getTitle, request.title().strip())
                .set(Notification::getContent, request.content())
                .set(request.readFlag() != null, Notification::getReadFlag, request.readFlag()));
        return get(id);
    }

    @Override
    public void delete(Long id) {
        if (notificationMapper.deleteById(id) == 0) {
            throw new BusinessException(404, "通知不存在");
        }
    }

    @Override
    public void deleteMine(Long userId, Long id) {
        if (notificationMapper.delete(ownNotification(userId, id)) == 0) {
            throw new BusinessException(404, "通知不存在");
        }
    }

    @Override
    public void markRead(Long userId, Long id) {
        int updated = notificationMapper.update(null, Wrappers.lambdaUpdate(Notification.class)
                .eq(Notification::getId, id)
                .eq(Notification::getUserId, requireCurrentUser(userId))
                .set(Notification::getReadFlag, READ));
        if (updated == 0) {
            getMine(userId, id);
        }
    }

    @Override
    @Transactional
    public void deliver(NotificationDeliveryRequest request) {
        notificationMapper.insertEvent(request.eventKey().strip(), request.userId(),
                request.title().strip(), request.content());
    }

    @Override
    public long count() {
        return notificationMapper.selectCount(null);
    }

    private LambdaQueryWrapper<Notification> ownNotification(Long userId, Long id) {
        return Wrappers.lambdaQuery(Notification.class)
                .eq(Notification::getId, id)
                .eq(Notification::getUserId, requireCurrentUser(userId));
    }

    private Long requireCurrentUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        return userId;
    }

    private Notification requireNotification(Notification notification) {
        if (notification == null) {
            throw new BusinessException(404, "通知不存在");
        }
        return notification;
    }

    private void validateRecipient(Long userId) {
        ApiResponse<Boolean> response;
        try {
            response = authUserClient.exists(userId);
        } catch (FeignException exception) {
            log.warn("校验通知收件人失败，userId={}，下游状态={}", userId, exception.status());
            throw new BusinessException(503, "暂时无法校验收件人，请稍后重试");
        }
        if (response == null || response.getCode() != 200) {
            throw new BusinessException(503, "暂时无法校验收件人，请稍后重试");
        }
        if (!Boolean.TRUE.equals(response.getData())) {
            throw new BusinessException("收件人账号不存在或已停用");
        }
    }
}
