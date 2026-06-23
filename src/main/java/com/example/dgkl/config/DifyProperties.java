package com.example.dgkl.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "dify")
public class DifyProperties {
    private String baseUrl = "https://api.dify.ai/v1";
    private String apiKey;
    private String responseMode = "blocking";
}
