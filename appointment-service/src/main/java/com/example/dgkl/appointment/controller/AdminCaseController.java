package com.example.dgkl.appointment.controller;

import com.example.dgkl.appointment.dto.LegalCaseRequest;
import com.example.dgkl.appointment.entity.LegalCase;
import com.example.dgkl.appointment.service.LegalCaseService;
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
@RequestMapping("/api/admin/cases")
@RequiredArgsConstructor
public class AdminCaseController {
    private final LegalCaseService legalCaseService;

    @GetMapping
    public ApiResponse<PageResult<LegalCase>> page(
            @RequestParam(defaultValue = "1") @Min(1) long pageNum,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long pageSize,
            @RequestParam(required = false) @Positive Long userId,
            @RequestParam(required = false) @Positive Long lawyerId) {
        return ApiResponse.success(legalCaseService.page(false, userId, lawyerId,
                pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<LegalCase> detail(@PathVariable @Positive Long id) {
        return ApiResponse.success(legalCaseService.detail(false, id));
    }

    @PostMapping
    public ApiResponse<LegalCase> create(@Valid @RequestBody LegalCaseRequest request) {
        return ApiResponse.success(legalCaseService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<LegalCase> update(@PathVariable @Positive Long id,
                                         @Valid @RequestBody LegalCaseRequest request) {
        return ApiResponse.success(legalCaseService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable @Positive Long id) {
        legalCaseService.delete(id);
        return ApiResponse.success();
    }
}
