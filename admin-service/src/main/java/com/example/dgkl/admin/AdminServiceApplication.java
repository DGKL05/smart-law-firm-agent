package com.example.dgkl.admin;

import com.example.dgkl.admin.client.StatsClients;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients(clients = {StatsClients.Auth.class, StatsClients.Lawyer.class, StatsClients.Appointment.class,
        StatsClients.Schedule.class, StatsClients.Notification.class, StatsClients.Ai.class})
@MapperScan("com.example.dgkl.admin.mapper")
@SpringBootApplication(scanBasePackages = {"com.example.dgkl.admin", "com.example.dgkl.common"})
public class AdminServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdminServiceApplication.class, args);
    }
}
