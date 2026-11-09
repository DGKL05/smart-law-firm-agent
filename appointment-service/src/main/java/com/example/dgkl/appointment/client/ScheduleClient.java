package com.example.dgkl.appointment.client;

import com.example.dgkl.appointment.dto.ScheduleReservationRequest;
import com.example.dgkl.appointment.dto.ScheduleSlotResponse;
import com.example.dgkl.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "schedule-service")
public interface ScheduleClient {
    @GetMapping("/internal/schedule/lawyers/{lawyerId}")
    ApiResponse<List<ScheduleSlotResponse>> slots(@PathVariable("lawyerId") Long lawyerId);

    @PostMapping("/internal/schedule/reservations")
    ApiResponse<Boolean> reserve(@RequestBody ScheduleReservationRequest request);

    @PostMapping("/internal/schedule/reservations/release")
    ApiResponse<Void> release(@RequestBody ScheduleReservationRequest request);
}
