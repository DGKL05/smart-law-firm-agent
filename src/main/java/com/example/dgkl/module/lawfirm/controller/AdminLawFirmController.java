package com.example.dgkl.module.lawfirm.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.dgkl.module.common.AbstractAdminCrudController;
import com.example.dgkl.module.lawfirm.entity.LawFirm;
import com.example.dgkl.module.lawfirm.service.LawFirmService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/law-firms")
@RequiredArgsConstructor
public class AdminLawFirmController extends AbstractAdminCrudController<LawFirm> {
    private final LawFirmService lawFirmService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    protected IService<LawFirm> service() {
        return lawFirmService;
    }

    @Override
    protected QueryWrapper<LawFirm> pageQuery(String keyword) {
        QueryWrapper<LawFirm> wrapper = new QueryWrapper<LawFirm>().orderByDesc("create_time");
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like("name", keyword).or().like("province_name", keyword);
        }
        return wrapper;
    }

    @Override
    public com.example.dgkl.common.Result<LawFirm> create(LawFirm entity) {
        com.example.dgkl.common.Result<LawFirm> result = super.create(entity);
        clearCache();
        return result;
    }

    @Override
    public com.example.dgkl.common.Result<LawFirm> update(Long id, LawFirm entity) {
        com.example.dgkl.common.Result<LawFirm> result = super.update(id, entity);
        clearCache();
        return result;
    }

    @Override
    public com.example.dgkl.common.Result<Void> delete(Long id) {
        com.example.dgkl.common.Result<Void> result = super.delete(id);
        clearCache();
        return result;
    }

    private void clearCache() {
        try {
            redisTemplate.delete(redisTemplate.keys("lawfirm:province:*"));
            redisTemplate.delete("home:stats");
        } catch (Exception ignored) {
        }
    }
}
