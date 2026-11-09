package com.example.dgkl.lawyer.controller;

import com.example.dgkl.common.response.ApiResponse;
import com.example.dgkl.lawyer.dto.LawyerSnapshotResponse;
import com.example.dgkl.lawyer.service.LawyerService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalLawyerController {
    private final LawyerService lawyerService;

    @GetMapping("/lawyers/{id}/snapshot")
    public ApiResponse<LawyerSnapshotResponse> snapshot(@PathVariable @Positive Long id) {
        return ApiResponse.success(lawyerService.snapshot(id));
    }

    @GetMapping("/lawyers/by-user/{userId}/snapshot")
    public ApiResponse<LawyerSnapshotResponse> snapshotByUserId(@PathVariable @Positive Long userId) {
        return ApiResponse.success(lawyerService.snapshotByUserId(userId));
    }

    @GetMapping("/admin/stats")
    public ApiResponse<Map<String, Long>> stats() {
        return ApiResponse.success(lawyerService.stats());
    }
}
