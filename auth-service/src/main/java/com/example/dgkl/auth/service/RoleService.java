package com.example.dgkl.auth.service;

import com.example.dgkl.auth.dto.RoleRequest;
import com.example.dgkl.auth.dto.RoleView;
import com.example.dgkl.common.response.PageResult;

public interface RoleService {
    PageResult<RoleView> listRoles(long pageNum, long pageSize, String keyword);

    RoleView getRole(Long id);

    RoleView createRole(RoleRequest request);

    RoleView updateRole(Long id, RoleRequest request);

    void deleteRole(Long id);
}
