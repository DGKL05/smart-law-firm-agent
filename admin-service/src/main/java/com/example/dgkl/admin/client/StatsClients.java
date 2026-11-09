package com.example.dgkl.admin.client;

import com.example.dgkl.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

public final class StatsClients {
    private StatsClients() {
    }

    public interface Client {
        @GetMapping("/internal/admin/stats")
        ApiResponse<Map<String, Long>> stats();
    }

    @FeignClient(name = "auth-service")
    public interface Auth extends Client {
    }

    @FeignClient(name = "lawyer-service")
    public interface Lawyer extends Client {
    }

    @FeignClient(name = "appointment-service")
    public interface Appointment extends Client {
    }

    @FeignClient(name = "schedule-service")
    public interface Schedule extends Client {
    }

    @FeignClient(name = "notification-service")
    public interface Notification extends Client {
    }

    @FeignClient(name = "ai-service")
    public interface Ai extends Client {
    }
}
