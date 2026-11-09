package com.example.dgkl.appointment.controller;

import com.example.dgkl.appointment.dto.ConsultationRequest;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/admin/consultations")
@RequiredArgsConstructor
public class AdminConsultationController {
    private final ConsultationService consultationService;

    @GetMapping
    public ApiResponse<PageResult<Consultation>> page(
            @RequestParam(defaultValue = "1") @Min(1) long pageNum,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long pageSize,
            @RequestParam(required = false) @Positive Long userId,
            @RequestParam(required = false) @Positive Long lawyerId) {
        return ApiResponse.success(consultationService.page(Access.ADMIN, userId, lawyerId,
                pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<Consultation> detail(@PathVariable @Positive Long id) {
        return ApiResponse.success(consultationService.detail(Access.ADMIN, id));
    }

    @PostMapping
    public ApiResponse<Consultation> create(@Valid @RequestBody ConsultationRequest request) {
        return ApiResponse.success(consultationService.create(Access.ADMIN, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Consultation> update(@PathVariable @Positive Long id,
                                            @Valid @RequestBody ConsultationRequest request) {
        return ApiResponse.success(consultationService.update(Access.ADMIN, id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable @Positive Long id) {
        consultationService.delete(Access.ADMIN, id);
        return ApiResponse.success();
    }

    @PutMapping("/{id}/close")
    public ApiResponse<Consultation> close(@PathVariable @Positive Long id) {
        return ApiResponse.success(consultationService.close(id));
    }
}
