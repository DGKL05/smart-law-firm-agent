package com.example.dgkl.appointment.controller;

import com.example.dgkl.appointment.dto.ConsultationReplyRequest;
import com.example.dgkl.appointment.entity.Consultation;
import com.example.dgkl.appointment.service.ConsultationService;
import com.example.dgkl.appointment.service.ConsultationService.Access;
import com.example.dgkl.common.response.ApiResponse;
import com.example.dgkl.common.response.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/lawyer/consultations")
@RequiredArgsConstructor
public class LawyerConsultationController {
    private final ConsultationService consultationService;

    @GetMapping
    public ApiResponse<PageResult<Consultation>> page(
            @RequestParam(defaultValue = "1") @Min(1) long pageNum,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long pageSize) {
        return ApiResponse.success(consultationService.page(Access.LAWYER, null, null,
                pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<Consultation> detail(@PathVariable @Positive Long id) {
        return ApiResponse.success(consultationService.detail(Access.LAWYER, id));
    }

    @PutMapping("/{id}/reply")
    public ApiResponse<Consultation> reply(@PathVariable @Positive Long id,
                                            @Valid @RequestBody ConsultationReplyRequest request) {
        return ApiResponse.success(consultationService.reply(id, request));
    }
}
