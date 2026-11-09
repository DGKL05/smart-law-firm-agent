package com.example.dgkl.schedule.client;

import com.example.dgkl.common.response.ApiResponse;
import com.example.dgkl.schedule.dto.LawyerSnapshot;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "lawyer-service")
public interface LawyerClient {
    @GetMapping("/internal/lawyers/{id}/snapshot")
    ApiResponse<LawyerSnapshot> snapshot(@PathVariable("id") Long id);

    @GetMapping("/internal/lawyers/by-user/{userId}/snapshot")
    ApiResponse<LawyerSnapshot> snapshotByUserId(@PathVariable("userId") Long userId);
}
