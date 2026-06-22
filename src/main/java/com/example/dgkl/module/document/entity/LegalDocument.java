package com.example.dgkl.module.document.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.dgkl.module.common.BaseEntity;
import com.example.dgkl.module.common.UserOwned;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("legal_document")
public class LegalDocument extends BaseEntity implements UserOwned {
    private Long userId;
    private String title;
    private String documentType;
    private String content;
    private String fileUrl;
    private String status;
}
