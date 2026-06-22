package com.example.dgkl.module.document.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dgkl.module.document.entity.LegalDocument;
import com.example.dgkl.module.document.mapper.LegalDocumentMapper;
import com.example.dgkl.module.document.service.LegalDocumentService;
import org.springframework.stereotype.Service;

@Service
public class LegalDocumentServiceImpl extends ServiceImpl<LegalDocumentMapper, LegalDocument> implements LegalDocumentService {
}
