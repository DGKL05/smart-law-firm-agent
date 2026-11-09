package com.example.dgkl.appointment.controller;

import com.example.dgkl.appointment.entity.LegalCase;
import com.example.dgkl.appointment.service.LegalCaseService;
import com.example.dgkl.common.response.ApiResponse;
import com.example.dgkl.common.response.PageResult;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/me/cases")
@RequiredArgsConstructor
public class UserCaseController {
    private final LegalCaseService legalCaseService;

    @GetMapping
    public ApiResponse<PageResult<LegalCase>> page(
            @RequestParam(defaultValue = "1") @Min(1) long pageNum,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long pageSize) {
        return ApiResponse.success(legalCaseService.page(true, null, null,
                pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<LegalCase> detail(@PathVariable @Positive Long id) {
        return ApiResponse.success(legalCaseService.detail(true, id));
    }
}
