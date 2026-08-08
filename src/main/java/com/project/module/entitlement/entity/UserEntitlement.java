package com.project.module.entitlement.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_entitlements")
public class UserEntitlement {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Integer type;         // 1-内容解锁 2-会员
    private Long contentId;
    private Long episodeId;
    private LocalDateTime expireTime;  // NULL表示永久

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
