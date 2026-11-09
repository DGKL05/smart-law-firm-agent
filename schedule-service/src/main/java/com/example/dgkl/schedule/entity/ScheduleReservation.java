package com.example.dgkl.schedule.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("schedule_reservation")
public class ScheduleReservation {
    @TableId(type = IdType.INPUT)
    private Long appointmentId;
    private Long lawyerId;
    private LocalDateTime slotStart;
    private String status;
}
