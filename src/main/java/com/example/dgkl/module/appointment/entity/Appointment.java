package com.example.dgkl.module.appointment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.dgkl.module.common.BaseEntity;
import com.example.dgkl.module.common.UserOwned;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("appointment")
public class Appointment extends BaseEntity implements UserOwned {
    private Long userId;
    private Long lawFirmId;
    private Long lawyerId;
    private String lawFirmName;
    private String lawyerName;
    private LocalDateTime appointmentTime;
    private String remark;
    private String status;
}
