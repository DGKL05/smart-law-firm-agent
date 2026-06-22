package com.example.dgkl.module.lawfirm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.dgkl.module.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("law_firm")
public class LawFirm extends BaseEntity {
    private String name;
    private String provinceCode;
    private String provinceName;
    private String city;
    private String address;
    private String phone;
    private String email;
    private String logoUrl;
    private String description;
    private String specialties;
    private String licenseNo;
    private Integer status;
}
