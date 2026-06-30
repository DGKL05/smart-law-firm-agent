package com.example.dgkl.module.log.service;

import com.example.dgkl.common.PageResult;
import com.example.dgkl.module.log.document.OperationLogDocument;

public interface OperationLogService {
    void saveQuietly(OperationLogDocument document);

    PageResult<OperationLogDocument> page(long pageNum, long pageSize);
}
