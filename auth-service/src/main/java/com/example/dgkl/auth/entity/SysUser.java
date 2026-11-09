package com.example.dgkl.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.dgkl.common.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {
    private String username;
    @JsonIgnore
    @ToString.Exclude
    private String password;
    private String nickname;
    private String phone;
    private String email;
    private Integer status;
}
