package com.example.dgkl.admin.service.impl;

import com.example.dgkl.admin.client.StatsClients;
import com.example.dgkl.admin.service.AdminStatsService;
import com.example.dgkl.common.response.ApiResponse;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AdminStatsServiceImpl implements AdminStatsService {
    private final Map<String, StatsClients.Client> clients = new LinkedHashMap<>();

    public AdminStatsServiceImpl(StatsClients.Auth auth, StatsClients.Lawyer lawyer,
                                 StatsClients.Appointment appointment, StatsClients.Schedule schedule,
                                 StatsClients.Notification notification, StatsClients.Ai ai) {
        clients.put("auth-service", auth);
        clients.put("lawyer-service", lawyer);
        clients.put("appointment-service", appointment);
        clients.put("schedule-service", schedule);
        clients.put("notification-service", notification);
        clients.put("ai-service", ai);
    }

    @Override
    public Map<String, Object> overview() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> unavailable = new ArrayList<>(clients.size());
        clients.forEach((name, client) -> {
            try {
                ApiResponse<Map<String, Long>> response = client.stats();
                if (response != null && response.getCode() == 200 && response.getData() != null) {
                    result.putAll(response.getData());
                } else {
                    unavailable.add(name);
                }
            } catch (FeignException exception) {
                log.warn("读取业务统计失败，服务={}，状态={}", name, exception.status());
                unavailable.add(name);
            }
        });
        result.put("unavailableServices", unavailable);
        return result;
    }
}
