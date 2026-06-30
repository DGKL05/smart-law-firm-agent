package com.example.dgkl.module.log.service.impl;

import com.example.dgkl.common.PageResult;
import com.example.dgkl.module.log.document.OperationLogDocument;
import com.example.dgkl.module.log.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
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

    @Override
    public PageResult<OperationLogDocument> page(long pageNum, long pageSize) {
        Query query = new Query().with(PageRequest.of(Math.max((int) pageNum - 1, 0),
                (int) pageSize, Sort.by(Sort.Direction.DESC, "createTime")));
        long total = mongoTemplate.count(new Query(), OperationLogDocument.class);
        return new PageResult<>(mongoTemplate.find(query, OperationLogDocument.class), total, pageNum, pageSize);
    }
}
