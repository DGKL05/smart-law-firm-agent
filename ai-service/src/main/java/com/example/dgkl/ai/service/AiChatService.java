package com.example.dgkl.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dgkl.ai.dto.AiChatRequest;
import com.example.dgkl.ai.dto.AiChatResponse;
import com.example.dgkl.ai.dto.AiChatSessionDetailResponse;
import com.example.dgkl.ai.dto.DifyChatMessageResponse;
import com.example.dgkl.ai.entity.AiChatMessage;
import com.example.dgkl.ai.entity.AiChatSession;
import com.example.dgkl.ai.mapper.AiChatMessageMapper;
import com.example.dgkl.ai.mapper.AiChatSessionMapper;
import com.example.dgkl.common.exception.BusinessException;
import com.example.dgkl.common.response.PageResult;
import com.example.dgkl.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiChatService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final AiChatSessionMapper sessionMapper;
    private final AiChatMessageMapper messageMapper;
    private final RestTemplate difyRestTemplate;

    @Value("${dify.base-url:https://api.dify.ai/v1}")
    private String difyBaseUrl;
    @Value("${dify.api-key:}")
    private String difyApiKey;

    public AiChatResponse chat(AiChatRequest request) {
        if (request == null || request.getQuery() == null || request.getQuery().isBlank()) {
            throw new BusinessException("请输入要咨询的问题");
        }
        Long userId = requireUserId();
        AiChatSession session = resolveSession(userId, request);
        saveMessage(userId, session.getId(), "user", request.getQuery());
        DifyChatMessageResponse dify = callDify(request, userId);
        AiChatResponse response = new AiChatResponse();
        response.setSessionId(session.getId());
        response.setAnswer(dify.getAnswer());
        response.setConversationId(dify.getConversationId());
        response.setMessageId(dify.getMessageId());
        response.setIntent("NORMAL_CHAT");
        saveMessage(userId, session.getId(), "assistant", response.getAnswer());
        session.setConversationId(response.getConversationId());
        session.setLastMessage(response.getAnswer());
        session.setTitle(buildTitle(session.getTitle(), request.getQuery()));
        sessionMapper.updateById(session);
        return response;
    }

    public PageResult<AiChatSession> sessions(long pageNum, long pageSize) {
        Long userId = requireUserId();
        Page<AiChatSession> page = sessionMapper.selectPage(Page.of(pageNum, pageSize), new LambdaQueryWrapper<AiChatSession>()
                .eq(AiChatSession::getUserId, userId)
                .eq(AiChatSession::getDeleted, 0)
                .orderByDesc(AiChatSession::getUpdateTime));
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public AiChatSessionDetailResponse detail(Long id) {
        Long userId = requireUserId();
        AiChatSession session = sessionMapper.selectById(id);
        if (session == null || !userId.equals(session.getUserId()) || Integer.valueOf(1).equals(session.getDeleted())) {
            throw new BusinessException(403, "只能访问自己的聊天记录");
        }
        List<AiChatMessage> messages = messageMapper.selectList(new LambdaQueryWrapper<AiChatMessage>()
                .eq(AiChatMessage::getSessionId, id)
                .eq(AiChatMessage::getDeleted, 0)
                .orderByAsc(AiChatMessage::getCreateTime));
        return AiChatSessionDetailResponse.of(session, messages);
    }

    public void delete(Long id) {
        Long userId = requireUserId();
        AiChatSession session = sessionMapper.selectById(id);
        if (session == null || !userId.equals(session.getUserId())) {
            throw new BusinessException(403, "只能删除自己的聊天记录");
        }
        session.setDeleted(1);
        sessionMapper.updateById(session);
    }

    private AiChatSession resolveSession(Long userId, AiChatRequest request) {
        if (request.getSessionId() != null) {
            AiChatSession existing = sessionMapper.selectById(request.getSessionId());
            if (existing != null && userId.equals(existing.getUserId()) && !Integer.valueOf(1).equals(existing.getDeleted())) {
                return existing;
            }
        }
        AiChatSession session = new AiChatSession();
        session.setUserId(userId);
        session.setTitle(buildTitle("", request.getQuery()));
        session.setConversationId(request.getConversationId());
        session.setLastMessage("");
        session.setDeleted(0);
        sessionMapper.insert(session);
        return session;
    }

    private void saveMessage(Long userId, Long sessionId, String role, String content) {
        AiChatMessage message = new AiChatMessage();
        message.setSessionId(sessionId);
        message.setUserId(userId);
        message.setRole(role);
        message.setContent(content == null ? "" : content);
        message.setCreateTime(LocalDateTime.now());
        message.setDeleted(0);
        messageMapper.insert(message);
    }

    private DifyChatMessageResponse callDify(AiChatRequest request, Long userId) {
        if (difyApiKey == null || difyApiKey.isBlank()) {
            throw new BusinessException(500, "请先配置 Dify API Key：DIFY_API_KEY");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(difyApiKey.trim());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("inputs", Map.of(
                "currentDateTime", LocalDateTime.now(ZONE).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                "timezone", ZONE.getId()
        ));
        body.put("query", request.getQuery().trim());
        body.put("response_mode", "blocking");
        body.put("conversation_id", request.getConversationId() == null ? "" : request.getConversationId());
        body.put("user", "user-" + userId);
        String url = difyBaseUrl.replaceAll("/+$", "") + "/chat-messages";
        return difyRestTemplate.postForObject(url, new HttpEntity<>(body, headers), DifyChatMessageResponse.class);
    }

    private String buildTitle(String current, String query) {
        if (current != null && !current.isBlank() && !"新会话".equals(current)) {
            return current;
        }
        return query == null || query.isBlank() ? "新会话" : query.trim().replaceAll("\\s+", " ").substring(0, Math.min(18, query.trim().length()));
    }

    private Long requireUserId() {
        Long userId = SecurityUtils.currentUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录后再使用智能法律助理");
        }
        return userId;
    }
}
