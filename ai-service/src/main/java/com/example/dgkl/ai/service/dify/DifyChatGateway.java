package com.example.dgkl.ai.service.dify;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.Tracer;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowException;
import com.example.dgkl.ai.dto.DifyChatMessageResponse;
import com.example.dgkl.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DifyChatGateway {
    public static final String RESOURCE = "ai:dify-chat";
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DIFY_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int CONVERSATION_ID_LENGTH = 128;
    private final RestTemplate difyRestTemplate;

    @Value("${dify.base-url:https://api.dify.ai/v1}")
    private String difyBaseUrl;
    @Value("${dify.api-key:}")
    private String difyApiKey;

    public void requireConfigured() {
        if (!StringUtils.hasText(difyApiKey)) {
            throw new BusinessException(503, "请先配置 Dify API Key：DIFY_API_KEY");
        }
    }

    public DifyChatMessageResponse send(String query, String conversationId, Long userId) {
        Entry entry = enter(userId);
        try {
            return callDify(query, conversationId, userId, entry);
        } finally {
            entry.exit(1, userId);
        }
    }

    private Entry enter(Long userId) {
        try {
            return SphU.entry(RESOURCE, EntryType.OUT, 1, userId);
        } catch (BlockException exception) {
            if (exception instanceof DegradeException) {
                throw new BusinessException(503, "智能助理服务暂不可用，请稍后重试");
            }
            if (exception instanceof ParamFlowException) {
                throw new BusinessException(429, "咨询过于频繁，请稍后重试");
            }
            throw new BusinessException(429, "智能助理正在处理较多请求，请稍后重试");
        }
    }

    private DifyChatMessageResponse callDify(String query, String conversationId, Long userId, Entry entry) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(difyApiKey.strip());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("inputs", Map.of("currentDateTime", LocalDateTime.now(ZONE).format(DIFY_TIME_FORMAT),
                "timezone", ZONE.getId()));
        body.put("query", query);
        body.put("response_mode", "blocking");
        body.put("conversation_id", conversationId == null ? "" : conversationId);
        body.put("user", "user-" + userId);
        String url = difyBaseUrl.replaceAll("/+$", "") + "/chat-messages";
        DifyChatMessageResponse response;
        try {
            response = difyRestTemplate.postForObject(url, new HttpEntity<>(body, headers),
                    DifyChatMessageResponse.class);
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            log.warn("Dify 返回异常 HTTP 状态 {}", status);
            if (exception.getStatusCode().is5xxServerError() || status == 429) {
                Tracer.traceEntry(exception, entry);
            }
            throw new BusinessException(502, "智能助理请求失败，请稍后重试");
        } catch (RestClientException exception) {
            Tracer.traceEntry(exception, entry);
            log.warn("无法调用 Dify，异常类型 {}", exception.getClass().getSimpleName());
            throw new BusinessException(503, "智能助理暂不可用，请稍后重试");
        }
        if (response == null || !StringUtils.hasText(response.getAnswer())
                || !StringUtils.hasText(response.getConversationId())
                || response.getConversationId().length() > CONVERSATION_ID_LENGTH) {
            Tracer.traceEntry(new IllegalStateException("Incomplete Dify response"), entry);
            throw new BusinessException(502, "智能助理未返回有效回复，请稍后重试");
        }
        return response;
    }
}
