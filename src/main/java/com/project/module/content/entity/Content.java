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
    private Long categoryId;
    private String tags;
    private Integer status;       // 0-下架 1-上架
    private Integer contentStatus; // 0草稿 1待审核 2审核中 3待上架 4上架 5下架 6删除 7驳回
    private String rejectReason;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewStartedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime publishedAt;
    private LocalDateTime offlineAt;
    private Long reviewedBy;
    private Long publishedBy;
    private Long offlineBy;
    private Long viewCount;
    private Long playCount;
    private Long uniqueViewCount;
    private Long playUserCount;
    private Long likeCount;
    private Long favoriteCount;
    private Long shareCount;
    private Long commentCount;
    private Long danmakuCount;
    private Integer sortOrder;

    private String adminRemark;
    private Long createdBy;
    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
