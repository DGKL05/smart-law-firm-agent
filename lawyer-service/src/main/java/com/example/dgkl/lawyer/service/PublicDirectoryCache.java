package com.example.dgkl.lawyer.service;

import com.example.dgkl.lawyer.entity.LawFirm;
import com.example.dgkl.lawyer.entity.Lawyer;
import com.example.dgkl.lawyer.mapper.LawFirmMapper;
import com.example.dgkl.lawyer.mapper.LawyerMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

@Slf4j
@Component
public class PublicDirectoryCache {
    private static final String LOADING_PREFIX = "@loading:";
    private static final Duration LOAD_LEASE = Duration.ofSeconds(5);
    private static final Duration READ_FAILURE_PAUSE = Duration.ofSeconds(2);
    // A concurrent database commit removes the marker so an older read cannot restore stale data.
    private static final DefaultRedisScript<Long> FINISH_LOAD = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) ~= ARGV[1] then return 0 end
            if ARGV[2] == '' then return redis.call('DEL', KEYS[1]) end
            redis.call('SET', KEYS[1], ARGV[2], 'PX', ARGV[3])
            return 1
            """, Long.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final LawFirmMapper lawFirmMapper;
    private final LawyerMapper lawyerMapper;
    private final String prefix;
    private final Duration ttl;
    private final AtomicLong bypassUntilMillis = new AtomicLong();

    public PublicDirectoryCache(StringRedisTemplate redis, ObjectMapper objectMapper, LawFirmMapper lawFirmMapper,
                                LawyerMapper lawyerMapper,
                                @Value("${NACOS_CONFIG_GROUP:SMART_LAW_DEV}") String environment,
                                @Value("${smart-law.directory-cache.ttl-seconds:120}") long ttlSeconds) {
        if (ttlSeconds < 1) {
            throw new IllegalArgumentException("公开目录缓存 TTL 必须为正数");
        }
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.lawFirmMapper = lawFirmMapper;
        this.lawyerMapper = lawyerMapper;
        this.prefix = "smart-law:directory:v1:" + environment + ":";
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    public LawFirm lawFirm(Long id) {
        return read(lawFirmKey(id), LawFirm.class, () -> lawFirmMapper.selectById(id));
    }

    public Lawyer lawyer(Long id) {
        return read(lawyerKey(id), Lawyer.class, () -> lawyerMapper.selectById(id));
    }

    public void invalidateLawFirm(Long id) {
        invalidateAfterCommit(lawFirmKey(id));
    }

    public void invalidateLawyer(Long id) {
        invalidateAfterCommit(lawyerKey(id));
    }

    private String lawFirmKey(Long id) {
        return prefix + "law-firm:" + id;
    }

    private String lawyerKey(Long id) {
        return prefix + "lawyer:" + id;
    }

    private <T> T read(String key, Class<T> type, Supplier<T> loadFromDatabase) {
        if (System.currentTimeMillis() < bypassUntilMillis.get()) {
            return loadFromDatabase.get();
        }
        String marker = null;
        try {
            String entry = redis.opsForValue().get(key);
            if (entry == null) {
                marker = claimLoad(key);
            } else if (!entry.startsWith(LOADING_PREFIX)) {
                T cached = objectMapper.readValue(entry, type);
                if (cached != null) {
                    return cached;
                }
                invalidate(key);
            }
        } catch (JsonProcessingException exception) {
            log.warn("目录缓存内容无法解析，key={}", key);
            invalidate(key);
        } catch (DataAccessException exception) {
            pause(READ_FAILURE_PAUSE, exception);
        }

        T value = loadFromDatabase.get();
        if (marker != null) {
            finishLoad(key, marker, value);
        }
        return value;
    }

    private String claimLoad(String key) {
        String marker = LOADING_PREFIX + UUID.randomUUID();
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, marker, LOAD_LEASE)) ? marker : null;
    }

    private void finishLoad(String key, String marker, Object value) {
        try {
            String json = value == null ? "" : objectMapper.writeValueAsString(value);
            redis.execute(FINISH_LOAD, List.of(key), marker, json, Long.toString(ttl.toMillis()));
        } catch (JsonProcessingException exception) {
            log.warn("目录缓存内容无法写入，key={}", key);
        } catch (DataAccessException exception) {
            pause(READ_FAILURE_PAUSE, exception);
        }
    }

    private void invalidateAfterCommit(String key) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    invalidate(key);
                }
            });
        } else {
            invalidate(key);
        }
    }

    private void invalidate(String key) {
        try {
            redis.delete(key);
        } catch (DataAccessException exception) {
            // A failed eviction can leave an old value in Redis until its original TTL expires.
            pause(ttl, exception);
        }
    }

    private void pause(Duration delay, DataAccessException exception) {
        long now = System.currentTimeMillis();
        long previous = bypassUntilMillis.getAndAccumulate(now + delay.toMillis(), Math::max);
        if (previous <= now) {
            log.warn("Redis 目录缓存暂不可用，回源查询：{}", exception.getClass().getSimpleName());
        }
    }
}
