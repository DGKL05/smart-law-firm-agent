package com.example.dgkl.lawyer.controller;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import com.example.dgkl.common.exception.BusinessException;
import com.example.dgkl.lawyer.service.PublicLawyerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.example.dgkl.lawyer.controller.PublicLawyerController.DETAIL_RESOURCE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PublicLawyerSentinelTest {
    private final PublicLawyerService service = mock(PublicLawyerService.class);
    private final PublicLawyerController controller = new PublicLawyerController(service);

    @AfterEach
    void clearRules() {
        FlowRuleManager.loadRules(List.of());
        ParamFlowRuleManager.loadRules(List.of());
        DegradeRuleManager.loadRules(List.of());
    }

    @Test
    void blocksWhenResourceFlowLimitIsExceeded() {
        FlowRule rule = new FlowRule(DETAIL_RESOURCE)
                .setGrade(RuleConstant.FLOW_GRADE_QPS)
                .setCount(0);
        FlowRuleManager.loadRules(List.of(rule));

        assertEquals(429, assertThrows(BusinessException.class, () -> controller.lawyerDetail(1L)).getCode());
    }

    @Test
    void blocksOnlyTheHotLawyerId() {
        ParamFlowRule rule = new ParamFlowRule(DETAIL_RESOURCE)
                .setParamIdx(0)
                .setCount(1)
                .setDurationInSec(10);
        ParamFlowRuleManager.loadRules(List.of(rule));

        controller.lawyerDetail(1L);
        assertEquals(429, assertThrows(BusinessException.class, () -> controller.lawyerDetail(1L)).getCode());
        assertDoesNotThrow(() -> controller.lawyerDetail(2L));
    }

    @Test
    void returnsFriendlyFallbackAfterRepeatedSlowCalls() {
        DegradeRule rule = new DegradeRule(DETAIL_RESOURCE)
                .setGrade(RuleConstant.DEGRADE_GRADE_RT)
                .setCount(5)
                .setSlowRatioThreshold(0.5)
                .setMinRequestAmount(2)
                .setStatIntervalMs(10_000)
                .setTimeWindow(10);
        DegradeRuleManager.loadRules(List.of(rule));
        when(service.lawyerDetail(3L)).thenAnswer(invocation -> {
            Thread.sleep(25);
            return null;
        });

        controller.lawyerDetail(3L);
        controller.lawyerDetail(3L);
        assertEquals(503, assertThrows(BusinessException.class, () -> controller.lawyerDetail(3L)).getCode());
    }
}
