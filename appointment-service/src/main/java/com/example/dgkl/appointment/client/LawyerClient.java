package com.example.dgkl.appointment.client;

import com.example.dgkl.appointment.dto.LawyerSnapshotResponse;
import com.example.dgkl.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "lawyer-service")
public interface LawyerClient {
    @GetMapping("/internal/lawyers/{id}/snapshot")
    ApiResponse<LawyerSnapshotResponse> snapshot(@PathVariable Long id);

    @GetMapping("/internal/lawyers/by-user/{userId}/snapshot")
    ApiResponse<LawyerSnapshotResponse> snapshotByUserId(@PathVariable Long userId);
}
