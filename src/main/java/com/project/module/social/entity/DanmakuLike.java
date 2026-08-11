package com.project.module.social.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("danmaku_likes")
public class DanmakuLike {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long danmakuId;
    private Long userId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
