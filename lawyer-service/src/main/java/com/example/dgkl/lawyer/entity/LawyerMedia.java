package com.example.dgkl.lawyer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("lawyer_media")
public class LawyerMedia {
    @TableId(type = IdType.INPUT)
    private String id;
    private String contentType;
    private byte[] imageData;
}
