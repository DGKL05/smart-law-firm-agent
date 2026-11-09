package com.example.dgkl.schedule.service;

import com.example.dgkl.common.response.PageResult;
import com.example.dgkl.schedule.dto.ScheduleBlockRequest;
import com.example.dgkl.schedule.dto.ScheduleReservationRequest;
import com.example.dgkl.schedule.dto.ScheduleRuleRequest;
import com.example.dgkl.schedule.dto.ScheduleSlotResponse;
import com.example.dgkl.schedule.entity.ScheduleBlock;
import com.example.dgkl.schedule.entity.ScheduleRule;

import java.util.List;

public interface ScheduleService {
    PageResult<ScheduleRule> rules(Long owner, Long lawyerId, long pageNum, long pageSize);

    ScheduleRule rule(Long id, Long owner);

    ScheduleRule createRule(ScheduleRuleRequest request, Long owner);

    ScheduleRule updateRule(Long id, ScheduleRuleRequest request, Long owner);

    void deleteRule(Long id, Long owner);

    PageResult<ScheduleBlock> blocks(Long owner, Long lawyerId, long pageNum, long pageSize);

    ScheduleBlock block(Long id, Long owner);

    ScheduleBlock createBlock(ScheduleBlockRequest request, Long owner);

    ScheduleBlock updateBlock(Long id, ScheduleBlockRequest request, Long owner);

    void deleteBlock(Long id, Long owner);

    List<ScheduleSlotResponse> slots(Long lawyerId);

    boolean reserve(ScheduleReservationRequest request);

    void release(ScheduleReservationRequest request);

    long ruleCount();
}
