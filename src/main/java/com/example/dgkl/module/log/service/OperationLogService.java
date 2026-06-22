package com.example.dgkl.module.log.service;

import com.example.dgkl.module.log.document.OperationLogDocument;

public interface OperationLogService {
    void saveQuietly(OperationLogDocument document);
}
