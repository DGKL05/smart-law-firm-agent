package com.example.dgkl.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("admin_audit_event")
public class AdminAuditEvent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long adminUserId;
    private String action;
    private String targetType;
    private String targetId;
    private String detail;
    private String source;
    private LocalDateTime createTime;
}
