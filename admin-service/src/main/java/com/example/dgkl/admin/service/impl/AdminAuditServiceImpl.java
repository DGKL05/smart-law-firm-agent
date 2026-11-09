package com.example.dgkl.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dgkl.admin.dto.AuditRequests;
import com.example.dgkl.admin.entity.AdminAuditEvent;
import com.example.dgkl.admin.mapper.AdminAuditEventMapper;
import com.example.dgkl.admin.service.AdminAuditService;
import com.example.dgkl.common.exception.BusinessException;
import com.example.dgkl.common.response.PageResult;
import com.example.dgkl.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminAuditServiceImpl implements AdminAuditService {
    private static final String MANUAL = "MANUAL";
    private static final String GATEWAY = "GATEWAY";
    private final AdminAuditEventMapper mapper;

    @Override
    public PageResult<AdminAuditEvent> page(long pageNum, long pageSize, String keyword) {
        LambdaQueryWrapper<AdminAuditEvent> query = new LambdaQueryWrapper<AdminAuditEvent>()
                .orderByDesc(AdminAuditEvent::getId);
        if (StringUtils.hasText(keyword)) {
            String term = keyword.strip();
            query.and(match -> match.likeRight(AdminAuditEvent::getAction, term)
                    .or().likeRight(AdminAuditEvent::getTargetType, term));
        }
        Page<AdminAuditEvent> page = mapper.selectPage(Page.of(pageNum, pageSize), query);
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public AdminAuditEvent detail(Long id) {
        AdminAuditEvent event = mapper.selectById(id);
        if (event == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND.value(), "审计记录不存在");
        }
        return event;
    }

    @Override
    public AdminAuditEvent create(AuditRequests.Manual request) {
        Long userId = SecurityUtils.currentUserId();
        if (userId == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED.value(), "请先登录");
        }
        return insert(userId, request.action(), request.targetType(), request.targetId(), request.detail(), MANUAL);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminAuditEvent update(Long id, AuditRequests.Edit request) {
        requireManual(id);
        String detail = request.detail() == null ? null : request.detail().strip();
        mapper.update(null, new LambdaUpdateWrapper<AdminAuditEvent>()
                .eq(AdminAuditEvent::getId, id)
                .eq(AdminAuditEvent::getSource, MANUAL)
                .set(AdminAuditEvent::getDetail, detail));
        return detail(id);
    }

    @Override
    public void delete(Long id) {
        requireManual(id);
        mapper.delete(new LambdaQueryWrapper<AdminAuditEvent>()
                .eq(AdminAuditEvent::getId, id)
                .eq(AdminAuditEvent::getSource, MANUAL));
    }

    @Override
    public void record(AuditRequests.Gateway request) {
        insert(request.adminUserId(), request.action(), request.targetType(), request.targetId(), request.detail(),
                GATEWAY);
    }

    private AdminAuditEvent insert(Long userId, String action, String targetType, String targetId, String detail,
                                   String source) {
        AdminAuditEvent event = new AdminAuditEvent();
        event.setAdminUserId(userId);
        event.setAction(action.strip());
        event.setTargetType(targetType);
        event.setTargetId(targetId);
        event.setDetail(detail);
        event.setSource(source);
        event.setCreateTime(LocalDateTime.now());
        mapper.insert(event);
        return event;
    }

    private void requireManual(Long id) {
        if (!MANUAL.equals(detail(id).getSource())) {
            throw new BusinessException(HttpStatus.FORBIDDEN.value(), "系统审计记录只读");
        }
    }
}
