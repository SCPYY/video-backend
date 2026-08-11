package com.project.module.social.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("danmaku")
public class Danmaku {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long episodeId;
    private Long userId;
    private String content;
    private Integer videoTime;    // 弹幕出现的时间点（秒）
    private String color;         // 弹幕颜色，默认 #FFFFFF
    private String position;      // 弹幕位置：scroll / fixed_top / fixed_bottom
    private Integer likeCount;
    private Integer status;       // 1-正常 2-已删除

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    // ---- 非数据库字段 ----
    @TableField(exist = false)
    private String nickname;

    @TableField(exist = false)
    private String avatarUrl;
}
