package com.example.dgkl.module.agent.service;

import com.example.dgkl.common.BusinessException;
import com.example.dgkl.config.DifyProperties;
import com.example.dgkl.module.agent.dto.DifyChatRequest;
import com.example.dgkl.module.agent.dto.DifyChatResponse;
import com.example.dgkl.module.agent.service.impl.DifyChatServiceImpl;
import com.example.dgkl.module.user.entity.SysUser;
import com.example.dgkl.security.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DifyChatServiceImplTest {
    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private DifyProperties properties;
    private DifyChatServiceImpl service;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        properties = new DifyProperties();
        properties.setBaseUrl("https://api.dify.ai/v1");
        properties.setApiKey("test-api-key");
        service = new DifyChatServiceImpl(restTemplate, properties);

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
    void sendsBlockingChatMessageToDifyWithApiKeyAndCurrentUser() {
        server.expect(requestTo("https://api.dify.ai/v1/chat-messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-api-key"))
                .andExpect(content().json("""
                        {
                          "inputs": {},
                          "query": "帮我预约 2026-06-29 15:00 在北京明德律师事务所找张明律师咨询",
                          "response_mode": "blocking",
                          "conversation_id": "conversation-1",
                          "user": "user-2"
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "event": "message",
                          "message_id": "message-1",
                          "conversation_id": "conversation-2",
                          "answer": "预约成功"
                        }
                        """, MediaType.APPLICATION_JSON));

        DifyChatRequest request = new DifyChatRequest();
        request.setQuery("帮我预约 2026-06-29 15:00 在北京明德律师事务所找张明律师咨询");
        request.setConversationId("conversation-1");

        DifyChatResponse response = service.chat(request);

        assertThat(response.getAnswer()).isEqualTo("预约成功");
        assertThat(response.getConversationId()).isEqualTo("conversation-2");
        assertThat(response.getMessageId()).isEqualTo("message-1");
        server.verify();
    }

    @Test
    void rejectsChatWhenApiKeyIsMissing() {
        properties.setApiKey("");
        DifyChatRequest request = new DifyChatRequest();
        request.setQuery("你好");

        assertThatThrownBy(() -> service.chat(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Dify API Key");
    }
}
