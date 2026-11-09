package com.example.dgkl.schedule.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.dgkl.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("schedule_block")
public class ScheduleBlock extends BaseEntity {
    private Long lawyerId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String reason;
}
