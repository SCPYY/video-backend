package com.project.module.message.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data @TableName("system_messages")
public class SystemMessage { @TableId(type=IdType.AUTO) private Long id; private String recipientType; private Long recipientId; private String messageType; private String actionType; private String title; private String content; private String targetType; private Long targetId; private String targetUrl; private String relatedType; private Long relatedId; private Integer isRead; private LocalDateTime readAt; @TableField(fill=FieldFill.INSERT) private LocalDateTime createdAt; }
