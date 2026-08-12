package com.project.module.content.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data @TableName("interactive_options")
public class InteractiveOption {
    @TableId(type = IdType.AUTO) private Long id;
    private Long nodeId; private Integer optionNo; private String title; private String description;
    private Long nextSceneId; private Long nextNodeId; private String conditionConfig; private String effectConfig;
    private Integer status;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
}
