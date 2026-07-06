package com.example.dgkl.appointment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.dgkl.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("appointment")
public class Appointment extends BaseEntity {
    private Long userId;
    private Long lawyerId;
    private Long lawFirmId;
    private String lawFirmName;
    private String lawyerName;
    private LocalDateTime appointmentTime;
    private Integer durationMinutes;
    private String status;
    private String remark;
}
