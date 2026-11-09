package com.example.dgkl.schedule.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.dgkl.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("lawyer_schedule")
public class ScheduleRule extends BaseEntity {
    private Long lawyerId;
    private Integer weekday;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer enabled;
}
