package com.example.dgkl.module.publicapi.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dgkl.common.PageResult;
import com.example.dgkl.common.Result;
import com.example.dgkl.module.lawfirm.entity.LawFirm;
import com.example.dgkl.module.lawfirm.service.LawFirmService;
import com.example.dgkl.module.lawyer.entity.Lawyer;
import com.example.dgkl.module.lawyer.service.LawyerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {
    private final LawFirmService lawFirmService;
    private final LawyerService lawyerService;
    private final RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/law-firms")
    public Result<PageResult<LawFirm>> lawFirms(@RequestParam(defaultValue = "1") long pageNum,
                                                @RequestParam(defaultValue = "10") long pageSize,
                                                @RequestParam(required = false) String provinceCode,
                                                @RequestParam(required = false) String keyword) {
        String cacheKey = provinceCode != null && !provinceCode.isBlank() && (keyword == null || keyword.isBlank())
                ? "lawfirm:province:" + provinceCode + ":" + pageNum + ":" + pageSize : null;
        PageResult<LawFirm> cached = readCache(cacheKey);
        if (cached != null) {
            return Result.success(cached);
        }
        QueryWrapper<LawFirm> wrapper = new QueryWrapper<LawFirm>().eq("status", 1).orderByDesc("create_time");
        if (provinceCode != null && !provinceCode.isBlank()) {
            wrapper.eq("province_code", provinceCode);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like("name", keyword);
        }
        PageResult<LawFirm> result = PageResult.of(lawFirmService.page(new Page<>(pageNum, pageSize), wrapper));
        writeCache(cacheKey, result);
        return Result.success(result);
    }

    @GetMapping("/law-firms/{id}")
    public Result<LawFirm> lawFirm(@PathVariable Long id) {
        return Result.success(lawFirmService.getById(id));
    }

    @GetMapping("/lawyers")
    public Result<PageResult<Lawyer>> lawyers(@RequestParam(defaultValue = "1") long pageNum,
                                              @RequestParam(defaultValue = "10") long pageSize,
                                              @RequestParam(required = false) String category,
                                              @RequestParam(required = false) Long lawFirmId,
                                              @RequestParam(required = false) String keyword) {
        String cacheKey = category != null && !category.isBlank() && lawFirmId == null && (keyword == null || keyword.isBlank())
                ? "lawyer:category:" + category + ":" + pageNum + ":" + pageSize : null;
        PageResult<Lawyer> cached = readCache(cacheKey);
        if (cached != null) {
            return Result.success(cached);
        }
        QueryWrapper<Lawyer> wrapper = new QueryWrapper<Lawyer>().eq("status", 1).orderByDesc("create_time");
        if (category != null && !category.isBlank()) {
            wrapper.eq("category", category);
        }
        if (lawFirmId != null) {
            wrapper.eq("law_firm_id", lawFirmId);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like("name", keyword).or().like("good_at", keyword);
        }
        PageResult<Lawyer> result = PageResult.of(lawyerService.page(new Page<>(pageNum, pageSize), wrapper));
        writeCache(cacheKey, result);
        return Result.success(result);
    }

    @GetMapping("/lawyers/{id}")
    public Result<Lawyer> lawyer(@PathVariable Long id) {
        return Result.success(lawyerService.getById(id));
    }

    @GetMapping("/home/stats")
    public Result<Map<String, Long>> homeStats() {
        String key = "home:stats";
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof Map<?, ?> map) {
                Object lawFirmCount = map.get("lawFirmCount");
                Object lawyerCount = map.get("lawyerCount");
                return Result.success(Map.of(
                        "lawFirmCount", lawFirmCount instanceof Number number ? number.longValue() : 0L,
                        "lawyerCount", lawyerCount instanceof Number number ? number.longValue() : 0L));
            }
        } catch (Exception ex) {
            log.warn("Redis home stats cache unavailable: {}", ex.getMessage());
        }
        Map<String, Long> stats = Map.of("lawFirmCount", lawFirmService.count(), "lawyerCount", lawyerService.count());
        try {
            redisTemplate.opsForValue().set(key, stats, Duration.ofMinutes(10));
        } catch (Exception ex) {
            log.warn("Redis home stats cache write failed: {}", ex.getMessage());
        }
        return Result.success(stats);
    }

    @SuppressWarnings("unchecked")
    private <T> PageResult<T> readCache(String cacheKey) {
        if (cacheKey == null) {
            return null;
        }
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof PageResult<?>) {
                return (PageResult<T>) cached;
            }
        } catch (Exception ex) {
            log.warn("Redis cache read failed: {}", ex.getMessage());
        }
        return null;
    }

    private void writeCache(String cacheKey, Object value) {
        if (cacheKey == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(cacheKey, value, Duration.ofMinutes(10));
        } catch (Exception ex) {
            log.warn("Redis cache write failed: {}", ex.getMessage());
        }
    }
}
