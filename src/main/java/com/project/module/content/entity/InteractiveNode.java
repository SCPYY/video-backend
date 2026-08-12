package com.project.module.content.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data @TableName("interactive_nodes")
public class InteractiveNode {
    @TableId(type = IdType.AUTO) private Long id;
    private Long sceneId; private Integer nodeNo; private String prompt; private String nodeType;
    private Integer showAt; private Integer timeoutSeconds; private Integer required;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
}
