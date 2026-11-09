package com.example.dgkl.ai.service.dify;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import com.example.dgkl.ai.dto.DifyChatMessageResponse;
import com.example.dgkl.common.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DifyChatGatewayTest {
    private final RestTemplate difyClient = mock(RestTemplate.class);
    private final DifyChatGateway gateway = new DifyChatGateway(difyClient);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(gateway, "difyBaseUrl", "http://dify.test/v1");
        ReflectionTestUtils.setField(gateway, "difyApiKey", "mock-api-key");
    }

    @AfterEach
    void clearRules() {
        FlowRuleManager.loadRules(List.of());
        ParamFlowRuleManager.loadRules(List.of());
        DegradeRuleManager.loadRules(List.of());
    }

    @Test
    void rateLimitUsesTheAuthenticatedUserParameter() {
        ParamFlowRule rule = new ParamFlowRule(DifyChatGateway.RESOURCE).setParamIdx(0)
                .setGrade(RuleConstant.FLOW_GRADE_QPS).setCount(3).setDurationInSec(10);
        ParamFlowRuleManager.loadRules(List.of(rule));
        when(difyClient.postForObject(anyString(), any(HttpEntity.class), eq(DifyChatMessageResponse.class)))
                .thenReturn(reply());

        for (int request = 0; request < 3; request++) {
            assertThat(gateway.send("question", null, 1L).getAnswer()).isEqualTo("answer");
        }
        assertThatThrownBy(() -> gateway.send("question", null, 1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        failure -> assertThat(failure.getCode()).isEqualTo(429));
        assertThat(gateway.send("question", null, 2L).getAnswer()).isEqualTo("answer");
        verify(difyClient, times(4))
                .postForObject(anyString(), any(HttpEntity.class), eq(DifyChatMessageResponse.class));
    }

    @Test
    void limitsSimultaneousCallsAcrossUsers() throws Exception {
        FlowRule rule = new FlowRule(DifyChatGateway.RESOURCE);
        rule.setGrade(RuleConstant.FLOW_GRADE_THREAD);
        rule.setCount(1);
        FlowRuleManager.loadRules(List.of(rule));
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch complete = new CountDownLatch(1);
        when(difyClient.postForObject(anyString(), any(HttpEntity.class), eq(DifyChatMessageResponse.class)))
                .thenAnswer(invocation -> {
                    entered.countDown();
                    assertThat(complete.await(5, TimeUnit.SECONDS)).isTrue();
                    return reply();
                });

        ExecutorService workers = Executors.newSingleThreadExecutor();
        try {
            var first = workers.submit(() -> gateway.send("first", null, 1L));
            assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> gateway.send("second", null, 2L)).isInstanceOfSatisfying(
                    BusinessException.class, failure -> assertThat(failure.getCode()).isEqualTo(429));
            complete.countDown();
            assertThat(first.get(5, TimeUnit.SECONDS).getAnswer()).isEqualTo("answer");
        } finally {
            complete.countDown();
            workers.shutdownNow();
        }
        verify(difyClient).postForObject(anyString(), any(HttpEntity.class), eq(DifyChatMessageResponse.class));
    }

    @Test
    void opensCircuitAfterUpstreamServerFailures() {
        configureCircuit();
        when(difyClient.postForObject(anyString(), any(HttpEntity.class), eq(DifyChatMessageResponse.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.BAD_GATEWAY));

        for (int request = 0; request < 5; request++) {
            assertThatThrownBy(() -> gateway.send("question", null, 1L)).isInstanceOfSatisfying(
                    BusinessException.class, failure -> assertThat(failure.getCode()).isEqualTo(502));
        }
        assertThatThrownBy(() -> gateway.send("question", null, 1L)).isInstanceOfSatisfying(
                BusinessException.class, failure -> assertThat(failure.getCode()).isEqualTo(503));
        verify(difyClient, times(5))
                .postForObject(anyString(), any(HttpEntity.class), eq(DifyChatMessageResponse.class));
    }

    @Test
    void individualUpstreamClientErrorsDoNotOpenTheSharedCircuit() {
        configureCircuit();
        when(difyClient.postForObject(anyString(), any(HttpEntity.class), eq(DifyChatMessageResponse.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        for (int request = 0; request < 6; request++) {
            assertThatThrownBy(() -> gateway.send("question", null, 1L)).isInstanceOfSatisfying(
                    BusinessException.class, failure -> assertThat(failure.getCode()).isEqualTo(502));
        }
        verify(difyClient, times(6))
                .postForObject(anyString(), any(HttpEntity.class), eq(DifyChatMessageResponse.class));
    }

    private void configureCircuit() {
        DegradeRule rule = new DegradeRule(DifyChatGateway.RESOURCE)
                .setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO)
                .setCount(0.5).setMinRequestAmount(5).setStatIntervalMs(10000).setTimeWindow(15);
        DegradeRuleManager.loadRules(List.of(rule));
    }

    private DifyChatMessageResponse reply() {
        DifyChatMessageResponse response = new DifyChatMessageResponse();
        response.setAnswer("answer");
        response.setConversationId("conversation-id");
        return response;
    }
}
