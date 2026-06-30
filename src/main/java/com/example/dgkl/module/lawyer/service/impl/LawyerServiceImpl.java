package com.example.dgkl.module.lawyer.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dgkl.module.lawyer.entity.Lawyer;
import com.example.dgkl.module.lawyer.mapper.LawyerMapper;
import com.example.dgkl.module.lawyer.service.LawyerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LawyerServiceImpl extends ServiceImpl<LawyerMapper, Lawyer> implements LawyerService {
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public boolean save(Lawyer entity) {
        boolean saved = super.save(entity);
        clearPublicCache();
        return saved;
    }

    @Override
    public boolean updateById(Lawyer entity) {
        boolean updated = super.updateById(entity);
        clearPublicCache();
        return updated;
    }

    @Override
    public boolean removeById(java.io.Serializable id) {
        boolean removed = super.removeById(id);
        clearPublicCache();
        return removed;
    }

    private void clearPublicCache() {
        try {
            redisTemplate.delete(redisTemplate.keys("lawyer:category:*"));
            redisTemplate.delete("home:stats");
        } catch (Exception ex) {
            log.warn("Lawyer public cache clear failed: {}", ex.getMessage());
        }
    }
}
