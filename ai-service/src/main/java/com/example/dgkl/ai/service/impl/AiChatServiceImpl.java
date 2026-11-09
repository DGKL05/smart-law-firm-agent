package com.example.dgkl.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dgkl.ai.dto.AiChatRequest;
import com.example.dgkl.ai.dto.AiChatResponse;
import com.example.dgkl.ai.dto.AiChatSessionDetailResponse;
import com.example.dgkl.ai.dto.AiChatSessionView;
import com.example.dgkl.ai.dto.DifyChatMessageResponse;
import com.example.dgkl.ai.entity.AiChatMessage;
import com.example.dgkl.ai.entity.AiChatSession;
import com.example.dgkl.ai.mapper.AiChatMessageMapper;
import com.example.dgkl.ai.mapper.AiChatSessionMapper;
import com.example.dgkl.ai.service.AiChatService;
import com.example.dgkl.ai.service.dify.DifyChatGateway;
import com.example.dgkl.ai.service.guard.AiChatFence;
import com.example.dgkl.ai.service.guard.RedisAiChatLock;
import com.example.dgkl.common.exception.BusinessException;
import com.example.dgkl.common.response.PageResult;
import com.example.dgkl.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final String DEFAULT_TITLE = "新会话";
    private static final int AUTO_TITLE_LENGTH = 18;
    private static final int MESSAGE_PREVIEW_LENGTH = 500;

    private final AiChatSessionMapper sessionMapper;
    private final AiChatMessageMapper messageMapper;
    private final DifyChatGateway dify;
    private final RedisAiChatLock chatLock;
    private final AiChatFence chatFence;
    private final TransactionTemplate transactionTemplate;

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        Long userId = requireUserId();
        Long sessionId = request.sessionId();
        String query = request.query().strip();
        if (sessionId != null) {
            ownedSession(sessionId, userId, false);
        }
        dify.requireConfigured();
        try (RedisAiChatLock.Lease lease = chatLock.acquire(userId, sessionId)) {
            chatFence.claim(lease);
            // 获得租约和数据库守卫后重新读取会话，使用服务器保存的 Dify 会话标识。
            String conversationId = sessionId == null
                    ? null : ownedSession(sessionId, userId, false).getConversationId();
            chatFence.requireCurrent(lease, false);
            DifyChatMessageResponse answer = dify.send(query, conversationId, userId);
            return transactionTemplate.execute(status -> {
                chatFence.requireCurrent(lease, true);
                AiChatResponse response = persistChat(sessionId, userId, query, conversationId, answer);
                lease.requireOwnership();
                return response;
            });
        }
    }

    @Override
    public PageResult<AiChatSessionView> sessions(long pageNum, long pageSize) {
        Long userId = requireUserId();
        Page<AiChatSession> page = sessionMapper.selectPage(Page.of(pageNum, pageSize),
                new LambdaQueryWrapper<AiChatSession>().eq(AiChatSession::getUserId, userId)
                        .orderByDesc(AiChatSession::getUpdateTime, AiChatSession::getId));
        List<AiChatSessionView> records = page.getRecords().stream().map(AiChatSessionView::of).toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public AiChatSessionView create(String title) {
        return AiChatSessionView.of(insertSession(requireUserId(), title.strip(), LocalDateTime.now(ZONE)));
    }

    @Override
    public AiChatSessionDetailResponse detail(Long id) {
        Long userId = requireUserId();
        AiChatSession session = ownedSession(id, userId, false);
        List<AiChatMessage> messages = messageMapper.selectList(messagesIn(id, userId)
                .orderByAsc(AiChatMessage::getId));
        return AiChatSessionDetailResponse.of(session, messages);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiChatSessionView rename(Long id, String title) {
        Long userId = requireUserId();
        AiChatSession session = ownedSession(id, userId, true);
        LocalDateTime now = LocalDateTime.now(ZONE);
        session.setTitle(title.strip());
        session.setUpdateTime(now);
        sessionMapper.update(null, ownedSessionUpdate(id, userId)
                .set(AiChatSession::getTitle, session.getTitle())
                .set(AiChatSession::getUpdateTime, now));
        return AiChatSessionView.of(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Long userId = requireUserId();
        ownedSession(id, userId, true);
        sessionMapper.deleteById(id);
        messageMapper.delete(messagesIn(id, userId));
    }

    @Override
    public long countSessions() {
        return sessionMapper.selectCount(null);
    }

    private AiChatResponse persistChat(Long sessionId, Long userId, String query, String previousConversationId,
                                       DifyChatMessageResponse difyResponse) {
        LocalDateTime now = LocalDateTime.now(ZONE);
        AiChatSession session = sessionId == null
                ? insertSession(userId, autoTitle(query), now) : ownedSession(sessionId, userId, true);
        if (!Objects.equals(session.getConversationId(), previousConversationId)) {
            throw new BusinessException(409, "会话已更新，请刷新后重试");
        }
        Long id = session.getId();
        String answer = difyResponse.getAnswer();
        String conversationId = difyResponse.getConversationId();
        saveMessage(userId, id, "user", query, now);
        saveMessage(userId, id, "assistant", answer, now);
        String title = DEFAULT_TITLE.equals(session.getTitle()) ? autoTitle(query) : session.getTitle();
        sessionMapper.update(null, ownedSessionUpdate(id, userId)
                .set(AiChatSession::getTitle, title)
                .set(AiChatSession::getConversationId, conversationId)
                .set(AiChatSession::getLastMessage, truncate(answer, MESSAGE_PREVIEW_LENGTH))
                .set(AiChatSession::getUpdateTime, now));
        AiChatResponse response = new AiChatResponse();
        response.setSessionId(id);
        response.setAnswer(answer);
        response.setConversationId(conversationId);
        response.setMessageId(difyResponse.getMessageId());
        response.setIntent("NORMAL_CHAT");
        return response;
    }

    private AiChatSession ownedSession(Long id, Long userId, boolean lock) {
        LambdaQueryWrapper<AiChatSession> query = new LambdaQueryWrapper<AiChatSession>()
                .eq(AiChatSession::getId, id)
                .eq(AiChatSession::getUserId, userId)
                .last(lock, "FOR UPDATE");
        AiChatSession session = sessionMapper.selectOne(query);
        if (session == null) {
            throw new BusinessException(404, "会话不存在");
        }
        return session;
    }

    private LambdaUpdateWrapper<AiChatSession> ownedSessionUpdate(Long id, Long userId) {
        return new LambdaUpdateWrapper<AiChatSession>()
                .eq(AiChatSession::getId, id)
                .eq(AiChatSession::getUserId, userId);
    }

    private LambdaQueryWrapper<AiChatMessage> messagesIn(Long sessionId, Long userId) {
        return new LambdaQueryWrapper<AiChatMessage>()
                .eq(AiChatMessage::getSessionId, sessionId)
                .eq(AiChatMessage::getUserId, userId);
    }

    private AiChatSession insertSession(Long userId, String title, LocalDateTime now) {
        AiChatSession session = new AiChatSession();
        session.setUserId(userId);
        session.setTitle(title);
        session.setLastMessage("");
        session.setCreateTime(now);
        session.setUpdateTime(now);
        session.setDeleted(0);
        sessionMapper.insert(session);
        return session;
    }

    private void saveMessage(Long userId, Long sessionId, String role, String content, LocalDateTime now) {
        AiChatMessage message = new AiChatMessage();
        message.setSessionId(sessionId);
        message.setUserId(userId);
        message.setRole(role);
        message.setContent(content);
        message.setCreateTime(now);
        message.setDeleted(0);
        messageMapper.insert(message);
    }

    private String autoTitle(String query) {
        return truncate(query.replaceAll("\\s+", " "), AUTO_TITLE_LENGTH);
    }

    private String truncate(String value, int maxLength) {
        int length = value.codePointCount(0, value.length());
        return length <= maxLength ? value : value.substring(0, value.offsetByCodePoints(0, maxLength));
    }

    private Long requireUserId() {
        Long userId = SecurityUtils.currentUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录后再使用智能法律助理");
        }
        return userId;
    }
}
