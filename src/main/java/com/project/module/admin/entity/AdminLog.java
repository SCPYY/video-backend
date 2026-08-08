package com.project.module.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("admin_logs")
public class AdminLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long adminId;
    private String action;     // CREATE / UPDATE / DELETE / UPLOAD / BATCH_CREATE
    private String module;     // CONTENT / EPISODE / UPLOAD / IMAGE
    private String targetId;
    private String beforeData; // JSON
    private String afterData;  // JSON
    private String ipAddress;
    private String userAgent;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
