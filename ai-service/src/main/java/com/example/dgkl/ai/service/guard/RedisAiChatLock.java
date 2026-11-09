package com.example.dgkl.ai.service.guard;

import com.example.dgkl.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisAiChatLock {
    private static final Duration LEASE_TIME = Duration.ofSeconds(120);
    private static final String KEY_PREFIX = "smart-law:ai:chat:";
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('get', KEYS[1]) == ARGV[1] then
                return redis.call('del', KEYS[1])
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redis;

    public Lease acquire(Long userId, Long sessionId) {
        String key = KEY_PREFIX + userId + ":" + (sessionId == null ? "new" : sessionId);
        String token = UUID.randomUUID().toString();
        Boolean acquired;
        try {
            acquired = redis.opsForValue().setIfAbsent(key, token, LEASE_TIME);
        } catch (DataAccessException exception) {
            throw unavailable(exception);
        }
        if (acquired == null) {
            throw new BusinessException(503, "会话锁暂不可用，请稍后重试");
        }
        if (!acquired) {
            throw new BusinessException(409, "此会话有请求正在处理，请稍后重试");
        }
        return new Lease(key, token);
    }

    private BusinessException unavailable(DataAccessException exception) {
        log.warn("无法使用 AI 会话锁，异常类型 {}", exception.getClass().getSimpleName());
        return new BusinessException(503, "会话锁暂不可用，请稍后重试");
    }

    public final class Lease implements AutoCloseable {
        private final String key;
        private final String token;

        private Lease(String key, String token) {
            this.key = key;
            this.token = token;
        }

        String key() {
            return key;
        }

        String token() {
            return token;
        }

        public void requireOwnership() {
            String current;
            try {
                current = redis.opsForValue().get(key);
            } catch (DataAccessException exception) {
                throw unavailable(exception);
            }
            if (!token.equals(current)) {
                throw new BusinessException(409, "会话处理超时，请刷新后重试");
            }
        }

        @Override
        public void close() {
            try {
                redis.execute(RELEASE_SCRIPT, List.of(key), token);
            } catch (DataAccessException exception) {
                // 锁设置了租约，释放失败时等待自然过期。不能删除其他请求接管的锁。
                log.warn("释放 AI 会话锁失败，异常类型 {}", exception.getClass().getSimpleName());
            }
        }
    }
}
