package com.project.module.social.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("comment_likes")
public class CommentLike {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long commentId;
    private Long userId;
    private Integer type;         // 1-点赞 2-点踩

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
