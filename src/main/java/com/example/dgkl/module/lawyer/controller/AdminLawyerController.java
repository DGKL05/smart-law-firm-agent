package com.example.dgkl.module.lawyer.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.dgkl.module.common.AbstractAdminCrudController;
import com.example.dgkl.module.lawyer.entity.Lawyer;
import com.example.dgkl.module.lawyer.service.LawyerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/lawyers")
@RequiredArgsConstructor
public class AdminLawyerController extends AbstractAdminCrudController<Lawyer> {
    private final LawyerService lawyerService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    protected IService<Lawyer> service() {
        return lawyerService;
    }

    @Override
    protected QueryWrapper<Lawyer> pageQuery(String keyword) {
        QueryWrapper<Lawyer> wrapper = new QueryWrapper<Lawyer>().orderByDesc("create_time");
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like("name", keyword).or().like("category", keyword);
        }
        return wrapper;
    }

    @Override
    public com.example.dgkl.common.Result<Lawyer> create(Lawyer entity) {
        com.example.dgkl.common.Result<Lawyer> result = super.create(entity);
        clearCache();
        return result;
    }

    @Override
    public com.example.dgkl.common.Result<Lawyer> update(Long id, Lawyer entity) {
        com.example.dgkl.common.Result<Lawyer> result = super.update(id, entity);
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
            redisTemplate.delete(redisTemplate.keys("lawyer:category:*"));
            redisTemplate.delete("home:stats");
        } catch (Exception ignored) {
        }
    }
}
