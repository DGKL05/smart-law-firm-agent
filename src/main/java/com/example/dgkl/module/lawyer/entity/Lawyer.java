package com.example.dgkl.module.lawyer.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.dgkl.module.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("lawyer")
public class Lawyer extends BaseEntity {
    private Long lawFirmId;
    private String name;
    private String gender;
    private String avatarUrl;
    private String phone;
    private String email;
    private String category;
    private String title;
    private Integer experienceYears;
    private String description;
    private String goodAt;
    private String availableTimeSlots;
    private Integer status;
}
