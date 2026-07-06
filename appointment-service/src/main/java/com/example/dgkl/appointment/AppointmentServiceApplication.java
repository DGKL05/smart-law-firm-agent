package com.example.dgkl.appointment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.mybatis.spring.annotation.MapperScan;

@EnableFeignClients
@MapperScan("com.example.dgkl.appointment.mapper")
@SpringBootApplication(scanBasePackages = {"com.example.dgkl.appointment", "com.example.dgkl.common"})
public class AppointmentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AppointmentServiceApplication.class, args);
    }
}
