package com.project.module.content.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data @TableName("interactive_scenes")
public class InteractiveScene {
    @TableId(type = IdType.AUTO) private Long id;
    private Long contentId; private Long episodeId; private Integer sceneNo; private String title;
    private String description; private String videoUrl; private Integer duration; private String sceneType;
    private Integer isStart; private Integer status; private Long viewCount; private Long playCount;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
}
