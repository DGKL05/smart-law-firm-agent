package com.example.dgkl.admin.controller;

import com.example.dgkl.admin.dto.AuditRequests;
import com.example.dgkl.admin.entity.AdminAuditEvent;
import com.example.dgkl.admin.service.AdminAuditService;
import com.example.dgkl.common.response.ApiResponse;
import com.example.dgkl.common.response.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/admin/audit-events")
@RequiredArgsConstructor
public class AdminAuditController {
    private final AdminAuditService service;

    @GetMapping
    public ApiResponse<PageResult<AdminAuditEvent>> page(@RequestParam(defaultValue = "1") @Min(1) long pageNum,
                                                        @RequestParam(defaultValue = "10") @Min(1) @Max(100)
                                                        long pageSize,
                                                        @RequestParam(required = false) @Size(max = 80)
                                                        String keyword) {
        return ApiResponse.success(service.page(pageNum, pageSize, keyword));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminAuditEvent> detail(@PathVariable @Positive Long id) {
        return ApiResponse.success(service.detail(id));
    }

    @PostMapping
    public ApiResponse<AdminAuditEvent> create(@Valid @RequestBody AuditRequests.Manual request) {
        return ApiResponse.success(service.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AdminAuditEvent> update(@PathVariable @Positive Long id,
                                               @Valid @RequestBody AuditRequests.Edit request) {
        return ApiResponse.success(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable @Positive Long id) {
        service.delete(id);
        return ApiResponse.success();
    }
}
