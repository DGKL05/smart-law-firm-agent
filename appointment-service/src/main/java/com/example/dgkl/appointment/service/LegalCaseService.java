package com.example.dgkl.appointment.service;

import com.example.dgkl.appointment.dto.LegalCaseRequest;
import com.example.dgkl.appointment.entity.LegalCase;
import com.example.dgkl.common.response.PageResult;

public interface LegalCaseService {
    PageResult<LegalCase> page(boolean mine, Long userId, Long lawyerId, long pageNum, long pageSize);

    LegalCase detail(boolean mine, Long id);

    LegalCase create(LegalCaseRequest request);

    LegalCase update(Long id, LegalCaseRequest request);

    void delete(Long id);

    long count();
}
