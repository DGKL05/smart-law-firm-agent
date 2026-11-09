package com.example.dgkl.appointment.service;

import com.example.dgkl.appointment.client.AuthClient;
import com.example.dgkl.appointment.client.LawyerClient;
import com.example.dgkl.appointment.dto.LawyerSnapshotResponse;
import com.example.dgkl.common.exception.BusinessException;
import com.example.dgkl.common.response.ApiResponse;
import com.example.dgkl.common.security.SecurityUtils;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentPeers {
    private final LawyerClient lawyerClient;
    private final AuthClient authClient;

    public Long currentUserId() {
        Long userId = SecurityUtils.currentUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        return userId;
    }

    public Long currentLawyerId() {
        Long userId = currentUserId();
        return lawyerLookup(() -> lawyerClient.snapshotByUserId(userId), "当前账号未绑定律师资料").getId();
    }

    public LawyerSnapshotResponse lawyer(Long lawyerId) {
        return lawyerLookup(() -> lawyerClient.snapshot(lawyerId), "律师不存在");
    }

    public void requireUser(Long userId) {
        if (userId == null) {
            throw new BusinessException("请选择用户");
        }
        try {
            ApiResponse<Boolean> response = authClient.exists(userId);
            if (response == null || response.getCode() != 200 || response.getData() == null) {
                throw new BusinessException(503, "暂时无法核验用户");
            }
            if (!response.getData()) {
                throw new BusinessException(404, "用户不存在或不可用");
            }
        } catch (FeignException exception) {
            log.warn("User lookup failed for id {}: HTTP {}", userId, exception.status());
            throw new BusinessException(503, "暂时无法核验用户");
        }
    }

    private LawyerSnapshotResponse lawyerLookup(Supplier<ApiResponse<LawyerSnapshotResponse>> request,
                                                String absentMessage) {
        try {
            ApiResponse<LawyerSnapshotResponse> response = request.get();
            if (response != null && response.getCode() == 200 && response.getData() != null) {
                return response.getData();
            }
            throw new BusinessException(404, absentMessage);
        } catch (FeignException exception) {
            if (exception.status() == 400 || exception.status() == 404) {
                throw new BusinessException(404, absentMessage);
            }
            log.warn("Lawyer lookup failed: HTTP {}", exception.status());
            throw new BusinessException(503, "律师服务暂不可用");
        }
    }
}
