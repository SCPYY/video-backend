package com.project.module.content.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data @TableName("content_status_logs")
public class ContentStatusLog { @TableId(type=IdType.AUTO) private Long id; private Long contentId; private Integer fromStatus; private Integer toStatus; private String action; private String reason; private Long operatorId; private String ipAddress; private String userAgent; @TableField(fill=FieldFill.INSERT) private LocalDateTime createdAt; }
