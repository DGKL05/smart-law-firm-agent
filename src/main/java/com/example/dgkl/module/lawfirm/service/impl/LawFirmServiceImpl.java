package com.example.dgkl.module.lawfirm.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dgkl.module.lawfirm.entity.LawFirm;
import com.example.dgkl.module.lawfirm.mapper.LawFirmMapper;
import com.example.dgkl.module.lawfirm.service.LawFirmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LawFirmServiceImpl extends ServiceImpl<LawFirmMapper, LawFirm> implements LawFirmService {
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public boolean save(LawFirm entity) {
        boolean saved = super.save(entity);
        clearPublicCache();
        return saved;
    }

    @Override
    public boolean updateById(LawFirm entity) {
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
            redisTemplate.delete(redisTemplate.keys("lawfirm:province:*"));
            redisTemplate.delete("home:stats");
        } catch (Exception ex) {
            log.warn("Law firm public cache clear failed: {}", ex.getMessage());
        }
    }
}
