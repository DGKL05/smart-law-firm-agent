package com.example.dgkl.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dgkl.common.exception.BusinessException;
import com.example.dgkl.common.response.PageResult;
import com.example.dgkl.notification.dto.NotificationTemplateRequest;
import com.example.dgkl.notification.entity.NotificationTemplate;
import com.example.dgkl.notification.mapper.NotificationTemplateMapper;
import com.example.dgkl.notification.service.NotificationTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class NotificationTemplateServiceImpl implements NotificationTemplateService {
    private final NotificationTemplateMapper templateMapper;

    @Override
    public PageResult<NotificationTemplate> list(int pageNum, int pageSize, String keyword) {
        LambdaQueryWrapper<NotificationTemplate> query = Wrappers.lambdaQuery(NotificationTemplate.class)
                .orderByDesc(NotificationTemplate::getId);
        if (StringUtils.hasText(keyword)) {
            query.and(condition -> condition.like(NotificationTemplate::getCode, keyword)
                    .or().like(NotificationTemplate::getTitle, keyword)
                    .or().like(NotificationTemplate::getContent, keyword));
        }
        Page<NotificationTemplate> page = templateMapper.selectPage(Page.of(pageNum, pageSize), query);
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public NotificationTemplate get(Long id) {
        return requireTemplate(templateMapper.selectById(id));
    }

    @Override
    @Transactional
    public NotificationTemplate create(NotificationTemplateRequest request) {
        NotificationTemplate template = new NotificationTemplate();
        template.setCode(request.code());
        template.setTitle(request.title().strip());
        template.setContent(request.content());
        try {
            templateMapper.insert(template);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(409, "通知模板编码已存在");
        }
        return get(template.getId());
    }

    @Override
    @Transactional
    public NotificationTemplate update(Long id, NotificationTemplateRequest request) {
        requireTemplate(templateMapper.selectOne(Wrappers.lambdaQuery(NotificationTemplate.class)
                .eq(NotificationTemplate::getId, id).last("FOR UPDATE")));
        try {
            templateMapper.update(null, Wrappers.lambdaUpdate(NotificationTemplate.class)
                    .eq(NotificationTemplate::getId, id)
                    .set(NotificationTemplate::getCode, request.code())
                    .set(NotificationTemplate::getTitle, request.title().strip())
                    .set(NotificationTemplate::getContent, request.content()));
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(409, "通知模板编码已存在");
        }
        return get(id);
    }

    @Override
    public void delete(Long id) {
        if (templateMapper.deleteById(id) == 0) {
            throw new BusinessException(404, "通知模板不存在");
        }
    }

    private NotificationTemplate requireTemplate(NotificationTemplate template) {
        if (template == null) {
            throw new BusinessException(404, "通知模板不存在");
        }
        return template;
    }
}
