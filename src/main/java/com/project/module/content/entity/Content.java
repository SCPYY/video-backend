package com.project.module.content.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("contents")
public class Content {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer type;         // 1-短剧 2-影游
    private String title;
    private String description;
    private String coverUrl;
    private String category;
    private String tags;
    private Integer status;       // 0-下架 1-上架
    private Long viewCount;
    private Integer sortOrder;

    private String adminRemark;
    private Long createdBy;
    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
