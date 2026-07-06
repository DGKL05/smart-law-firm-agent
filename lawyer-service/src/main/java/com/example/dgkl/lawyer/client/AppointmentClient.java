package com.example.dgkl.lawyer.client;

import com.example.dgkl.common.response.ApiResponse;
import com.example.dgkl.lawyer.dto.ScheduleSlotResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "appointment-service")
public interface AppointmentClient {
    @GetMapping("/internal/schedule/lawyers/{lawyerId}")
    ApiResponse<List<ScheduleSlotResponse>> schedule(@PathVariable Long lawyerId);
}
