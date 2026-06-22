package com.example.dgkl.module.log.service.impl;

import com.example.dgkl.module.log.document.OperationLogDocument;
import com.example.dgkl.module.log.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {
    private final MongoTemplate mongoTemplate;

    @Override
    public void saveQuietly(OperationLogDocument document) {
        try {
            mongoTemplate.save(document);
        } catch (Exception ex) {
            log.warn("MongoDB operation log write failed: {}", ex.getMessage());
        }
    }
}
