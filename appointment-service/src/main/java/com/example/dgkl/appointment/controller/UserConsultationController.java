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
@RequestMapping("/api/me/consultations")
@RequiredArgsConstructor
public class UserConsultationController {
    private final ConsultationService consultationService;

    @GetMapping
    public ApiResponse<PageResult<Consultation>> page(
            @RequestParam(defaultValue = "1") @Min(1) long pageNum,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long pageSize) {
        return ApiResponse.success(consultationService.page(Access.USER, null, null,
                pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<Consultation> detail(@PathVariable @Positive Long id) {
        return ApiResponse.success(consultationService.detail(Access.USER, id));
    }

    @PostMapping
    public ApiResponse<Consultation> create(@Valid @RequestBody ConsultationRequest request) {
        return ApiResponse.success(consultationService.create(Access.USER, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Consultation> update(@PathVariable @Positive Long id,
                                            @Valid @RequestBody ConsultationRequest request) {
        return ApiResponse.success(consultationService.update(Access.USER, id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable @Positive Long id) {
        consultationService.delete(Access.USER, id);
        return ApiResponse.success();
    }
}
