package com.example.dgkl.lawyer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.mybatis.spring.annotation.MapperScan;

@EnableFeignClients
@MapperScan("com.example.dgkl.lawyer.mapper")
@SpringBootApplication(scanBasePackages = {"com.example.dgkl.lawyer", "com.example.dgkl.common"})
public class LawyerServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(LawyerServiceApplication.class, args);
    }
}
