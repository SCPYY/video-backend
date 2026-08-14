package com.project.module.content.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("interactive_choices")
public class InteractiveChoice {
    @TableId(type = IdType.AUTO) private Long id;
    private Long userId;
    private Long contentId;
    private Long sceneId;
    private Long nodeId;
    private Long optionId;
    private Long nextSceneId;
    private Long nextNodeId;
    private Integer videoPosition;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
}
