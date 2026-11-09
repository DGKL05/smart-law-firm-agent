package com.example.dgkl.admin.controller;

import com.example.dgkl.admin.dto.AuditRequests;
import com.example.dgkl.admin.service.AdminAuditService;
import com.example.dgkl.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/admin/audit-events")
@RequiredArgsConstructor
public class InternalAuditController {
    private final AdminAuditService service;

    @PostMapping
    public ApiResponse<Void> record(@Valid @RequestBody AuditRequests.Gateway request) {
        service.record(request);
        return ApiResponse.success();
    }
}
