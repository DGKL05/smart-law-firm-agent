package com.example.dgkl.module.ai.service.impl;

import com.example.dgkl.common.BusinessException;
import com.example.dgkl.config.DifyProperties;
import com.example.dgkl.module.agent.dto.AgentAppointmentRequest;
import com.example.dgkl.module.agent.service.AgentAppointmentService;
import com.example.dgkl.module.ai.dto.AiChatRequest;
import com.example.dgkl.module.ai.dto.AiChatResponse;
import com.example.dgkl.module.ai.dto.DifyChatMessageRequest;
import com.example.dgkl.module.ai.dto.DifyChatMessageResponse;
import com.example.dgkl.module.ai.dto.DifyIntentResponse;
import com.example.dgkl.module.ai.service.AiChatService;
import com.example.dgkl.security.SecurityUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {
    private static final ZoneId DIFY_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter APPOINTMENT_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final RestTemplate difyRestTemplate;
    private final DifyProperties difyProperties;
    private final ObjectMapper objectMapper;
    private final AgentAppointmentService agentAppointmentService;
    private final Clock aiClock;

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        validateRequest(request);
        Long userId = requireUserId();
        DifyChatMessageResponse difyResponse = callDify(request, userId);
        String rawAnswer = trimToEmpty(difyResponse.getAnswer());
        DifyIntentResponse intent = parseIntent(rawAnswer);
        if (intent == null) {
            return response(rawAnswer, difyResponse, "NORMAL_CHAT");
        }
        return handleIntent(intent, rawAnswer, difyResponse);
    }

    private DifyChatMessageResponse callDify(AiChatRequest request, Long userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(difyProperties.getApiKey().trim());

        try {
            ResponseEntity<DifyChatMessageResponse> response = difyRestTemplate.postForEntity(
                    chatMessagesUrl(),
                    new HttpEntity<>(difyRequest(request, userId), headers),
                    DifyChatMessageResponse.class);
            if (response.getBody() == null) {
                throw new BusinessException(500, "Dify 返回为空");
            }
            return response.getBody();
        } catch (RestClientResponseException ex) {
            throw new BusinessException(500, "Dify 调用失败：" + ex.getResponseBodyAsString());
        } catch (RestClientException ex) {
            throw new BusinessException(500, "Dify 调用失败：" + ex.getMessage());
        }
    }

    private DifyChatMessageRequest difyRequest(AiChatRequest request, Long userId) {
        LocalDateTime now = LocalDateTime.now(aiClock.withZone(DIFY_ZONE));
        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("currentDate", now.format(DATE_FORMAT));
        inputs.put("currentDateTime", now.format(DATE_TIME_FORMAT));
        inputs.put("timezone", DIFY_ZONE.getId());

        DifyChatMessageRequest difyRequest = new DifyChatMessageRequest();
        difyRequest.setInputs(inputs);
        difyRequest.setQuery(request.getQuery().trim());
        difyRequest.setResponseMode(defaultResponseMode());
        difyRequest.setConversationId(blankToEmpty(request.getConversationId()));
        difyRequest.setUser("user-" + userId);
        return difyRequest;
    }

    private AiChatResponse handleIntent(DifyIntentResponse intent, String rawAnswer, DifyChatMessageResponse difyResponse) {
        String intentName = trimToEmpty(intent.getIntent());
        if ("CREATE_APPOINTMENT".equals(intentName)) {
            return createAppointment(intent, difyResponse);
        }
        if ("CANCEL_APPOINTMENT".equals(intentName)) {
            return cancelAppointment(intent, difyResponse);
        }
        if ("MISSING_INFO".equals(intentName)) {
            return response(defaultReply(intent, rawAnswer), difyResponse, "MISSING_INFO");
        }
        return response(defaultReply(intent, rawAnswer), difyResponse, "NORMAL_CHAT");
    }

    private AiChatResponse createAppointment(DifyIntentResponse intent, DifyChatMessageResponse difyResponse) {
        try {
            AgentAppointmentRequest request = toAppointmentRequest(intent);
            agentAppointmentService.create(request);
            return response("预约成功：你已预约 " + intent.getAppointmentTime() + "，"
                    + intent.getLawFirmName() + "，" + intent.getLawyerName() + " 律师。", difyResponse, "CREATE_APPOINTMENT");
        } catch (BusinessException ex) {
            return response("预约失败：" + ex.getMessage(), difyResponse, "CREATE_APPOINTMENT");
        }
    }

    private AiChatResponse cancelAppointment(DifyIntentResponse intent, DifyChatMessageResponse difyResponse) {
        try {
            AgentAppointmentRequest request = toAppointmentRequest(intent);
            agentAppointmentService.cancel(request);
            return response("取消预约成功：已取消 " + intent.getAppointmentTime() + "，"
                    + intent.getLawFirmName() + "，" + intent.getLawyerName() + " 律师的咨询预约。", difyResponse, "CANCEL_APPOINTMENT");
        } catch (BusinessException ex) {
            return response("取消预约失败：" + ex.getMessage(), difyResponse, "CANCEL_APPOINTMENT");
        }
    }

    private AgentAppointmentRequest toAppointmentRequest(DifyIntentResponse intent) {
        if (isBlank(intent.getAppointmentTime()) || isBlank(intent.getLawFirmName()) || isBlank(intent.getLawyerName())) {
            throw new BusinessException("预约时间、律所名称和律师姓名不能为空");
        }
        AgentAppointmentRequest request = new AgentAppointmentRequest();
        request.setAppointmentTime(parseAppointmentTime(intent.getAppointmentTime()));
        request.setLawFirmName(intent.getLawFirmName().trim());
        request.setLawyerName(intent.getLawyerName().trim());
        request.setRemark(isBlank(intent.getRemark()) ? "用户通过智能法律助理预约法律咨询" : intent.getRemark().trim());
        return request;
    }

    private LocalDateTime parseAppointmentTime(String appointmentTime) {
        try {
            return LocalDateTime.parse(appointmentTime.trim(), APPOINTMENT_TIME_FORMAT);
        } catch (DateTimeParseException ex) {
            throw new BusinessException("预约时间格式必须为 yyyy-MM-dd HH:mm");
        }
    }

    private DifyIntentResponse parseIntent(String rawAnswer) {
        String json = cleanJsonAnswer(rawAnswer);
        if (json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, DifyIntentResponse.class);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private String cleanJsonAnswer(String answer) {
        String value = trimToEmpty(answer);
        if (value.startsWith("```")) {
            value = value.replaceFirst("^```(?:json)?\\s*", "");
            value = value.replaceFirst("\\s*```$", "");
        }
        if (value.startsWith("json")) {
            value = value.substring(4).trim();
        }
        return value.trim();
    }

    private AiChatResponse response(String answer, DifyChatMessageResponse difyResponse, String intent) {
        AiChatResponse response = new AiChatResponse();
        response.setAnswer(answer);
        response.setConversationId(difyResponse.getConversationId());
        response.setMessageId(difyResponse.getMessageId());
        response.setIntent(intent);
        return response;
    }

    private String defaultReply(DifyIntentResponse intent, String rawAnswer) {
        return isBlank(intent.getReply()) ? rawAnswer : intent.getReply();
    }

    private void validateRequest(AiChatRequest request) {
        if (request == null || isBlank(request.getQuery())) {
            throw new BusinessException("请输入要咨询的问题");
        }
        if (isBlank(difyProperties.getApiKey())) {
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
        String baseUrl = isBlank(difyProperties.getBaseUrl()) ? "https://api.dify.ai/v1" : difyProperties.getBaseUrl();
        return baseUrl.replaceAll("/+$", "") + "/chat-messages";
    }

    private String defaultResponseMode() {
        return isBlank(difyProperties.getResponseMode()) ? "blocking" : difyProperties.getResponseMode().trim();
    }

    private String blankToEmpty(String value) {
        return isBlank(value) ? "" : value.trim();
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
