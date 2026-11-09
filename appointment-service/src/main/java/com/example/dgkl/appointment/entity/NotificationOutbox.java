package com.example.dgkl.appointment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("appointment_notification_outbox")
public class NotificationOutbox {
    @TableId(type = IdType.INPUT)
    private String eventKey;
    private Long userId;
    private String title;
    private String content;
    private LocalDateTime nextAttemptTime;
    private LocalDateTime sentTime;
}
