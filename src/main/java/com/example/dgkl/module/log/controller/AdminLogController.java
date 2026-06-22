package com.example.dgkl.module.log.controller;

import com.example.dgkl.common.PageResult;
import com.example.dgkl.common.Result;
import com.example.dgkl.module.log.document.OperationLogDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/logs")
@RequiredArgsConstructor
public class AdminLogController {
    private final MongoTemplate mongoTemplate;

    @GetMapping
    public Result<PageResult<OperationLogDocument>> page(@RequestParam(defaultValue = "1") int pageNum,
                                                         @RequestParam(defaultValue = "10") int pageSize) {
        Query query = new Query().with(PageRequest.of(Math.max(pageNum - 1, 0), pageSize, Sort.by(Sort.Direction.DESC, "createTime")));
        long total = mongoTemplate.count(new Query(), OperationLogDocument.class);
        return Result.success(new PageResult<>(mongoTemplate.find(query, OperationLogDocument.class), total, pageNum, pageSize));
    }
}
