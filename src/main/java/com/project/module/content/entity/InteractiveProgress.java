package com.project.module.content.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("interactive_progress")
public class InteractiveProgress {
    @TableId(type = IdType.AUTO) private Long id;
    private Long userId;
    private Long contentId;
    private Long currentSceneId;
    private Long currentNodeId;
    private Long lastOptionId;
    private Integer progressSeconds;
    private Integer isFinished;
    private Long endingSceneId;
    private LocalDateTime lastPlayedAt;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
}
