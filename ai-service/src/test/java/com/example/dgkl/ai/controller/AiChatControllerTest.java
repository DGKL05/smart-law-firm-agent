package com.example.dgkl.ai.controller;

import com.example.dgkl.ai.entity.AiChatSession;
import com.example.dgkl.ai.mapper.AiChatMessageMapper;
import com.example.dgkl.ai.mapper.AiChatSessionMapper;
import com.example.dgkl.common.exception.GlobalExceptionHandler;
import com.example.dgkl.common.security.SecurityUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AiChatControllerTest.TestConfig.class)
@TestPropertySource(properties = "dify.api-key=")
class AiChatControllerTest {
    @MockBean
    private AiChatSessionMapper sessionMapper;
    @MockBean
    private AiChatMessageMapper messageMapper;
    @MockBean
    private RestTemplate difyRestTemplate;
    @MockBean
    private TransactionTemplate transactionTemplate;
    @MockBean
    private StringRedisTemplate redis;
    @MockBean
    private JdbcTemplate jdbc;
    @Autowired
    private AiChatController controller;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        SecurityUser principal = new SecurityUser(17L, "alice", "", List.of("USER"), true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createEmptySessionUsesAuthenticatedOwnerAndDoesNotCallDify() throws Exception {
        doAnswer(invocation -> {
            AiChatSession session = invocation.getArgument(0);
            session.setId(43L);
            return 1;
        }).when(sessionMapper).insert(any(AiChatSession.class));

        mvc.perform(post("/api/ai/sessions").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"咨询准备","userId":99}
                                """))
                .andExpect(jsonPath("$.data.id").value(43))
                .andExpect(jsonPath("$.data.title").value("咨询准备"))
                .andExpect(jsonPath("$.data.userId").value(17));

        verifyNoInteractions(messageMapper, difyRestTemplate);
    }

    @Test
    void missingDifyKeyLeavesNoSessionOrMessageBehind() throws Exception {
        mvc.perform(post("/api/ai/chat").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query":"如何整理咨询材料"}
                                """))
                .andExpect(jsonPath("$.message").value("请先配置 Dify API Key：DIFY_API_KEY"));

        verifyNoInteractions(sessionMapper, messageMapper, difyRestTemplate);
    }

    @Test
    void inaccessibleSessionIsRejectedBeforeExternalCallOrCreatingNewSession() throws Exception {
        mvc.perform(post("/api/ai/chat").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sessionId":43,"query":"如何整理咨询材料"}
                                """))
                .andExpect(jsonPath("$.code").value(404));

        verify(sessionMapper, never()).insert(any(AiChatSession.class));
        verifyNoInteractions(messageMapper, difyRestTemplate, transactionTemplate);
    }

    @Configuration(proxyBeanMethods = false)
    @Import(AiChatController.class)
    @ComponentScan("com.example.dgkl.ai.service")
    static class TestConfig {
    }
}
