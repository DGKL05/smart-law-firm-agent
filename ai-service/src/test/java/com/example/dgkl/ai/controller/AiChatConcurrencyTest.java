package com.example.dgkl.ai.controller;

import com.example.dgkl.ai.dto.DifyChatMessageResponse;
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
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AiChatConcurrencyTest.TestConfig.class)
@TestPropertySource(properties = "dify.api-key=mock-api-key")
class AiChatConcurrencyTest {
    private static final String LOCK_KEY = "smart-law:ai:chat:17:new";

    @MockBean
    private AiChatSessionMapper sessionMapper;
    @MockBean
    private AiChatMessageMapper messageMapper;
    @MockBean
    private RestTemplate difyRestTemplate;
    @MockBean
    private TransactionTemplate transactionTemplate;
    @MockBean
    private JdbcTemplate jdbc;
    @MockBean
    private StringRedisTemplate redis;
    @MockBean
    private ValueOperations<String, String> values;
    @Autowired
    private AiChatController controller;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        SecurityUser user = new SecurityUser(17L, "alice", "", List.of("USER"), true);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
        when(redis.opsForValue()).thenReturn(values);
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void busyNewConversationReturnsConflictWithoutCallingDifyOrWriting() throws Exception {
        when(values.setIfAbsent(eq(LOCK_KEY), anyString(), eq(Duration.ofSeconds(120)))).thenReturn(false);

        mvc.perform(post("/api/ai/chat").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query":"如何整理材料"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));

        verifyNoInteractions(difyRestTemplate, transactionTemplate, sessionMapper, messageMapper, jdbc);
    }

    @Test
    void redisFailureReturnsUnavailableWithoutCallingDifyOrWriting() throws Exception {
        when(values.setIfAbsent(eq(LOCK_KEY), anyString(), eq(Duration.ofSeconds(120))))
                .thenThrow(new RedisConnectionFailureException("mock Redis unavailable"));

        mvc.perform(post("/api/ai/chat").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query":"如何整理材料"}
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(503));

        verifyNoInteractions(difyRestTemplate, transactionTemplate, sessionMapper, messageMapper, jdbc);
    }

    @Test
    void replyFromFormerDatabaseOwnerCannotCreateSessionOrMessages() throws Exception {
        AtomicReference<String> token = new AtomicReference<>();
        when(values.setIfAbsent(eq(LOCK_KEY), anyString(), eq(Duration.ofSeconds(120)))).thenAnswer(invocation -> {
            token.set(invocation.getArgument(1));
            return true;
        });
        when(values.get(LOCK_KEY)).thenAnswer(invocation -> token.get());
        when(jdbc.queryForObject("SELECT owner_token FROM ai_chat_guard WHERE guard_key = ?", String.class, LOCK_KEY))
                .thenAnswer(invocation -> token.get());
        when(jdbc.queryForObject("SELECT owner_token FROM ai_chat_guard WHERE guard_key = ? FOR UPDATE",
                String.class, LOCK_KEY)).thenReturn("a-different-owner");
        TransactionStatus transaction = mock(TransactionStatus.class);
        doAnswer(invocation -> {
            Consumer<TransactionStatus> callback = invocation.getArgument(0);
            callback.accept(transaction);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(transaction);
        });
        DifyChatMessageResponse reply = new DifyChatMessageResponse();
        reply.setAnswer("已返回结果");
        reply.setConversationId("conversation-for-former-owner");
        when(difyRestTemplate.postForObject(anyString(), any(HttpEntity.class), eq(DifyChatMessageResponse.class)))
                .thenReturn(reply);

        mvc.perform(post("/api/ai/chat").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query":"如何整理材料"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("会话处理已被其他请求接管，请刷新后重试"));

        verifyNoInteractions(sessionMapper, messageMapper);
    }

    @Configuration(proxyBeanMethods = false)
    @Import(AiChatController.class)
    @ComponentScan("com.example.dgkl.ai.service")
    static class TestConfig {
    }
}
