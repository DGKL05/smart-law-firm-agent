package com.example.dgkl.module.ai.service;

import com.example.dgkl.common.BusinessException;
import com.example.dgkl.config.DifyProperties;
import com.example.dgkl.module.agent.dto.AgentAppointmentRequest;
import com.example.dgkl.module.agent.service.AgentAppointmentService;
import com.example.dgkl.module.ai.dto.AiChatRequest;
import com.example.dgkl.module.ai.dto.AiChatResponse;
import com.example.dgkl.module.ai.service.impl.AiChatServiceImpl;
import com.example.dgkl.module.appointment.entity.Appointment;
import com.example.dgkl.module.user.entity.SysUser;
import com.example.dgkl.security.LoginUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class AiChatServiceImplTest {
    @Mock
    private AgentAppointmentService agentAppointmentService;

    private MockRestServiceServer server;
    private AiChatServiceImpl service;
    private DifyProperties properties;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        properties = new DifyProperties();
        properties.setBaseUrl("https://api.dify.ai/v1");
        properties.setApiKey("test-api-key");
        properties.setResponseMode("blocking");
        Clock fixedClock = Clock.fixed(Instant.parse("2026-06-23T10:30:00Z"), ZoneId.of("Asia/Shanghai"));
        service = new AiChatServiceImpl(restTemplate, properties, new ObjectMapper(), agentAppointmentService, fixedClock);

        SysUser user = new SysUser();
        user.setId(2L);
        user.setUsername("user");
        LoginUser loginUser = new LoginUser(user, List.of("USER"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsAppointmentLocallyWhenDifyReturnsCreateIntent() {
        server.expect(requestTo("https://api.dify.ai/v1/chat-messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-api-key"))
                .andExpect(content().json("""
                        {
                          "inputs": {
                            "currentDate": "2026-06-23",
                            "currentDateTime": "2026-06-23 18:30:00",
                            "timezone": "Asia/Shanghai"
                          },
                          "query": "帮我预约 2026-06-23 14:00 黑龙江冰城律师事务所赵思远律师",
                          "response_mode": "blocking",
                          "conversation_id": "conversation-1",
                          "user": "user-2"
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "message_id": "message-1",
                          "conversation_id": "conversation-2",
                          "answer": "{\\"intent\\":\\"CREATE_APPOINTMENT\\",\\"appointmentTime\\":\\"2026-06-23 14:00\\",\\"lawFirmName\\":\\"黑龙江冰城律师事务所\\",\\"lawyerName\\":\\"赵思远\\",\\"remark\\":\\"用户通过智能法律助理预约法律咨询\\",\\"missingFields\\":[],\\"reply\\":\\"已识别到你的预约信息，正在为你创建预约。\\"}"
                        }
                        """, MediaType.APPLICATION_JSON));
        Appointment appointment = new Appointment();
        appointment.setAppointmentTime(LocalDateTime.of(2026, 6, 23, 14, 0));
        when(agentAppointmentService.create(any())).thenReturn(appointment);

        AiChatResponse response = service.chat(request(
                "帮我预约 2026-06-23 14:00 黑龙江冰城律师事务所赵思远律师",
                "conversation-1"));

        ArgumentCaptor<AgentAppointmentRequest> captor = ArgumentCaptor.forClass(AgentAppointmentRequest.class);
        verify(agentAppointmentService).create(captor.capture());
        assertThat(captor.getValue().getAppointmentTime()).isEqualTo(LocalDateTime.of(2026, 6, 23, 14, 0));
        assertThat(captor.getValue().getLawFirmName()).isEqualTo("黑龙江冰城律师事务所");
        assertThat(captor.getValue().getLawyerName()).isEqualTo("赵思远");
        assertThat(response.getAnswer()).isEqualTo("预约成功：你已预约 2026-06-23 14:00，黑龙江冰城律师事务所，赵思远 律师。");
        assertThat(response.getConversationId()).isEqualTo("conversation-2");
        assertThat(response.getMessageId()).isEqualTo("message-1");
        assertThat(response.getIntent()).isEqualTo("CREATE_APPOINTMENT");
        server.verify();
    }

    @Test
    void keepsConversationIdAndDoesNotTouchDatabaseWhenInformationIsMissing() {
        server.expect(requestTo("https://api.dify.ai/v1/chat-messages"))
                .andExpect(content().json("""
                        {
                          "conversation_id": "",
                          "user": "user-2"
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "message_id": "message-2",
                          "conversation_id": "conversation-3",
                          "answer": "{\\"intent\\":\\"MISSING_INFO\\",\\"appointmentTime\\":\\"\\",\\"lawFirmName\\":\\"黑龙江冰城律师事务所\\",\\"lawyerName\\":\\"赵思远\\",\\"remark\\":\\"\\",\\"missingFields\\":[\\"appointmentTime\\"],\\"reply\\":\\"预约法律咨询还需要补充准确到分钟的预约时间，例如 2026-06-23 14:00。\\"}"
                        }
                        """, MediaType.APPLICATION_JSON));

        AiChatResponse response = service.chat(request("帮我预约黑龙江冰城律师事务所赵思远律师的法律咨询", ""));

        verify(agentAppointmentService, never()).create(any());
        verify(agentAppointmentService, never()).cancel(any());
        assertThat(response.getAnswer()).isEqualTo("预约法律咨询还需要补充准确到分钟的预约时间，例如 2026-06-23 14:00。");
        assertThat(response.getConversationId()).isEqualTo("conversation-3");
        assertThat(response.getIntent()).isEqualTo("MISSING_INFO");
        server.verify();
    }

    @Test
    void fallsBackToNormalChatWhenDifyAnswerIsNotJson() {
        server.expect(requestTo("https://api.dify.ai/v1/chat-messages"))
                .andRespond(withSuccess("""
                        {
                          "message_id": "message-3",
                          "conversation_id": "conversation-4",
                          "answer": "这是一段普通回复"
                        }
                        """, MediaType.APPLICATION_JSON));

        AiChatResponse response = service.chat(request("我被公司无故辞退，可以主张哪些权利？", "conversation-3"));

        verify(agentAppointmentService, never()).create(any());
        verify(agentAppointmentService, never()).cancel(any());
        assertThat(response.getAnswer()).isEqualTo("这是一段普通回复");
        assertThat(response.getIntent()).isEqualTo("NORMAL_CHAT");
        assertThat(response.getConversationId()).isEqualTo("conversation-4");
        server.verify();
    }

    @Test
    void returnsBusinessMessageWhenLocalAppointmentCreationFails() {
        server.expect(requestTo("https://api.dify.ai/v1/chat-messages"))
                .andRespond(withSuccess("""
                        {
                          "message_id": "message-4",
                          "conversation_id": "conversation-5",
                          "answer": "{\\"intent\\":\\"CREATE_APPOINTMENT\\",\\"appointmentTime\\":\\"2026-06-23 14:00\\",\\"lawFirmName\\":\\"黑龙江冰城律师事务所\\",\\"lawyerName\\":\\"赵思远\\",\\"remark\\":\\"\\",\\"missingFields\\":[],\\"reply\\":\\"正在创建预约\\"}"
                        }
                        """, MediaType.APPLICATION_JSON));
        when(agentAppointmentService.create(any())).thenThrow(new BusinessException("该时间段不可预约，请选择其他时间"));

        AiChatResponse response = service.chat(request("帮我预约", ""));

        assertThat(response.getAnswer()).isEqualTo("预约失败：该时间段不可预约，请选择其他时间");
        assertThat(response.getIntent()).isEqualTo("CREATE_APPOINTMENT");
        assertThat(response.getConversationId()).isEqualTo("conversation-5");
        server.verify();
    }

    private AiChatRequest request(String query, String conversationId) {
        AiChatRequest request = new AiChatRequest();
        request.setQuery(query);
        request.setConversationId(conversationId);
        return request;
    }
}
