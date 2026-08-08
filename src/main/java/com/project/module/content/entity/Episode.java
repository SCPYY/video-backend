package com.project.module.content.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("episodes")
public class Episode {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long contentId;
    private Integer episodeNumber;
    private String title;
    private String videoUrl;
    private Integer duration;     // 时长（秒）
    private String interactiveConfig;  // JSON
    private Integer isFree;       // 0-付费 1-免费
    private Integer sortOrder;

    private Integer sourceType;       // 视频来源: 1-本地上传 2-第三方URL
    private Integer transcodeStatus;  // 转码状态: 0-未转码 1-转码中 2-已完成 3-失败
    private String originalFilename;
    private Long fileSize;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
