package com.example.dgkl.admin.service;

import com.example.dgkl.admin.dto.AuditRequests;
import com.example.dgkl.admin.entity.AdminAuditEvent;
import com.example.dgkl.common.response.PageResult;

public interface AdminAuditService {
    PageResult<AdminAuditEvent> page(long pageNum, long pageSize, String keyword);

    AdminAuditEvent detail(Long id);

    AdminAuditEvent create(AuditRequests.Manual request);

    AdminAuditEvent update(Long id, AuditRequests.Edit request);

    void delete(Long id);

    void record(AuditRequests.Gateway request);
}
