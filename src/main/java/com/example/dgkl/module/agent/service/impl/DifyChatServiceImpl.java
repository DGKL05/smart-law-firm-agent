package com.example.dgkl.module.agent.service.impl;

import com.example.dgkl.common.BusinessException;
import com.example.dgkl.config.DifyProperties;
import com.example.dgkl.module.agent.dto.DifyChatRequest;
import com.example.dgkl.module.agent.dto.DifyChatResponse;
import com.example.dgkl.module.agent.service.DifyChatService;
import com.example.dgkl.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DifyChatServiceImpl implements DifyChatService {
    private final RestTemplate difyRestTemplate;
    private final DifyProperties difyProperties;

    @Override
    public DifyChatResponse chat(DifyChatRequest request) {
        validateRequest(request);
        Long userId = requireUserId();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("inputs", request.getInputs() == null ? Map.of() : request.getInputs());
        payload.put("query", request.getQuery().trim());
        payload.put("response_mode", "blocking");
        payload.put("conversation_id", blankToEmpty(request.getConversationId()));
        payload.put("user", "user-" + userId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(difyProperties.getApiKey().trim());

        try {
            ResponseEntity<Map> response = difyRestTemplate.postForEntity(
                    chatMessagesUrl(),
                    new HttpEntity<>(payload, headers),
                    Map.class);
            return toResponse(response.getBody());
        } catch (RestClientResponseException ex) {
            throw new BusinessException(500, "Dify 调用失败：" + ex.getResponseBodyAsString());
        } catch (RestClientException ex) {
            throw new BusinessException(500, "Dify 调用失败：" + ex.getMessage());
        }
    }

    private void validateRequest(DifyChatRequest request) {
        if (request == null || request.getQuery() == null || request.getQuery().isBlank()) {
            throw new BusinessException("请输入要咨询的问题");
        }
        if (difyProperties.getApiKey() == null || difyProperties.getApiKey().isBlank()) {
            throw new BusinessException(500, "请先配置 Dify API Key：DIFY_API_KEY");
        }
    }

    private Long requireUserId() {
        Long userId = SecurityUtils.currentUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录后再使用智能法律助理");
        }
        return userId;
    }

    private String chatMessagesUrl() {
        String baseUrl = difyProperties.getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.dify.ai/v1";
        }
        return baseUrl.replaceAll("/+$", "") + "/chat-messages";
    }

    private DifyChatResponse toResponse(Map<String, Object> body) {
        if (body == null) {
            throw new BusinessException(500, "Dify 返回为空");
        }
        DifyChatResponse response = new DifyChatResponse();
        response.setEvent(asString(body.get("event")));
        response.setTaskId(asString(body.get("task_id")));
        response.setMessageId(asString(body.get("message_id")));
        response.setConversationId(asString(body.get("conversation_id")));
        response.setWorkflowRunId(asString(body.get("workflow_run_id")));
        response.setAnswer(asString(body.get("answer")));
        Object metadata = body.get("metadata");
        if (metadata instanceof Map<?, ?> metadataMap) {
            response.setMetadata((Map<String, Object>) metadataMap);
        }
        return response;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String blankToEmpty(String value) {
        return value == null || value.isBlank() ? "" : value;
    }
}
