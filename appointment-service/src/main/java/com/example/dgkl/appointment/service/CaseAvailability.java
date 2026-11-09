package com.example.dgkl.appointment.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.dgkl.appointment.entity.LegalCase;
import com.example.dgkl.appointment.mapper.LegalCaseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CaseAvailability {
    public static final String ACTIVE = "ACTIVE";
    public static final String CLOSED = "CLOSED";

    private final LegalCaseMapper caseMapper;

    public List<LegalCase> overlapping(Long lawyerId, LocalDateTime start, LocalDateTime end) {
        return caseMapper.selectList(Wrappers.<LegalCase>lambdaQuery()
                .eq(LegalCase::getLawyerId, lawyerId)
                .eq(LegalCase::getStatus, ACTIVE)
                .lt(LegalCase::getStartTime, end)
                .gt(LegalCase::getEndTime, start));
    }
}
