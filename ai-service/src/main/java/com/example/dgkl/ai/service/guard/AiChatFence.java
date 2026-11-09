package com.example.dgkl.ai.service.guard;

import com.example.dgkl.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@RequiredArgsConstructor
public class AiChatFence {
    private static final String OWNER_QUERY = "SELECT owner_token FROM ai_chat_guard WHERE guard_key = ?";

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactionTemplate;

    public void claim(RedisAiChatLock.Lease lease) {
        transactionTemplate.executeWithoutResult(status -> {
            // 插入或锁定既有守卫行，使 Redis 所有权校验和数据库持有者更新串行执行。
            jdbc.update("""
                    INSERT INTO ai_chat_guard (guard_key, owner_token) VALUES (?, ?)
                    ON DUPLICATE KEY UPDATE owner_token = ai_chat_guard.owner_token
                    """, lease.key(), lease.token());
            lease.requireOwnership();
            jdbc.update("UPDATE ai_chat_guard SET owner_token = ? WHERE guard_key = ?", lease.token(), lease.key());
        });
    }

    public void requireCurrent(RedisAiChatLock.Lease lease, boolean forWrite) {
        String owner;
        try {
            // 写入路径在同一事务内持有行锁直到提交，防止新持有者跨过这次检查。
            String sql = forWrite ? OWNER_QUERY + " FOR UPDATE" : OWNER_QUERY;
            owner = jdbc.queryForObject(sql, String.class, lease.key());
        } catch (EmptyResultDataAccessException exception) {
            throw lostOwnership();
        }
        if (!lease.token().equals(owner)) {
            throw lostOwnership();
        }
        lease.requireOwnership();
    }

    private BusinessException lostOwnership() {
        return new BusinessException(409, "会话处理已被其他请求接管，请刷新后重试");
    }
}
