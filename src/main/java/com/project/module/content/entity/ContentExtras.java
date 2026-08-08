package com.project.module.content.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("content_extras")
public class ContentExtras {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long contentId;

    @TableField("`key`")
    private String key;

    @TableField("`value`")
    private String value;         // JSON

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
