package com.example.dgkl.appointment.service;

import com.example.dgkl.appointment.dto.ConsultationReplyRequest;
import com.example.dgkl.appointment.dto.ConsultationRequest;
import com.example.dgkl.appointment.entity.Consultation;
import com.example.dgkl.common.response.PageResult;

public interface ConsultationService {
    enum Access {
        USER, LAWYER, ADMIN
    }

    PageResult<Consultation> page(Access access, Long userId, Long lawyerId, long pageNum, long pageSize);

    Consultation detail(Access access, Long id);

    Consultation create(Access access, ConsultationRequest request);

    Consultation update(Access access, Long id, ConsultationRequest request);

    void delete(Access access, Long id);

    Consultation reply(Long id, ConsultationReplyRequest request);

    Consultation close(Long id);

    long count();
}
