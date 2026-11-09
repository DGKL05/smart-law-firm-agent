package com.example.dgkl.gateway;

import com.example.dgkl.common.security.InternalServiceToken;
import com.example.dgkl.common.security.OAuth2IntrospectionSettings;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({InternalServiceToken.class, OAuth2IntrospectionSettings.class})
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
