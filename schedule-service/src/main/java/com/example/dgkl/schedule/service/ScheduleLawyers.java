package com.example.dgkl.schedule.service;

import com.example.dgkl.common.exception.BusinessException;
import com.example.dgkl.common.response.ApiResponse;
import com.example.dgkl.common.security.SecurityUtils;
import com.example.dgkl.schedule.client.LawyerClient;
import com.example.dgkl.schedule.dto.LawyerSnapshot;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleLawyers {
    private final LawyerClient lawyerClient;

    public Long currentId() {
        Long userId = SecurityUtils.currentUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        return lookup(() -> lawyerClient.snapshotByUserId(userId), "当前账号未绑定律师资料").id();
    }

    public Long forCreate(Long requestedId, Long owner) {
        if (owner != null) {
            requireUnchanged(requestedId, owner);
            return owner;
        }
        if (requestedId == null) {
            throw new BusinessException("请选择律师");
        }
        return lookup(() -> lawyerClient.snapshot(requestedId), "律师不存在").id();
    }

    public void requireUnchanged(Long requestedId, Long existingId) {
        if (requestedId != null && !requestedId.equals(existingId)) {
            throw new BusinessException("不可更换排班所属律师");
        }
    }

    private LawyerSnapshot lookup(Supplier<ApiResponse<LawyerSnapshot>> request, String absentMessage) {
        try {
            ApiResponse<LawyerSnapshot> response = request.get();
            if (response != null && response.getCode() == 200 && response.getData() != null) {
                return response.getData();
            }
            throw new BusinessException(404, absentMessage);
        } catch (FeignException exception) {
            if (exception.status() == 400 || exception.status() == 404) {
                throw new BusinessException(404, absentMessage);
            }
            log.warn("Lawyer lookup failed while managing schedules: HTTP {}", exception.status());
            throw new BusinessException(503, "律师服务暂不可用");
        }
    }
}
