package com.project.module.social.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("comments")
public class Comment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long contentId;
    private Long episodeId;
    private Long userId;
    private Long parentId;
    private Long rootId;
    private Long replyToUserId;
    private String content;
    private Integer likeCount;
    private Integer dislikeCount;
    private Integer replyCount;
    private Integer status;       // 1-正常 2-已删除 3-审核中
    private String ipAddress;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // ---- 非数据库字段 ----
    @TableField(exist = false)
    private String nickname;

    @TableField(exist = false)
    private String avatarUrl;

    @TableField(exist = false)
    private String replyToNickname;
}
