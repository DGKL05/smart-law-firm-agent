package com.example.dgkl.module.log.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "operation_log")
public class OperationLogDocument {
    @Id
    private String id;
    private Long userId;
    private String username;
    private String module;
    private String operation;
    private String requestUri;
    private String requestMethod;
    private String requestParams;
    private String ip;
    private Boolean success;
    private String errorMessage;
    private LocalDateTime createTime;
}
