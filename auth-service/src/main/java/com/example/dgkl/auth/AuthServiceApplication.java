package com.example.dgkl.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;

@MapperScan("com.example.dgkl.auth.mapper")
@SpringBootApplication(scanBasePackages = {"com.example.dgkl.auth", "com.example.dgkl.common"})
public class AuthServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
