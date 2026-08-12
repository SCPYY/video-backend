package com.project.module.content.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("content_categories")
public class ContentCategory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer type; // 1-短剧 2-影游
    private String name;
    private String description;
    private String iconUrl;
    private Integer sortOrder;
    private Integer status; // 0-禁用 1-启用
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
