package com.project.module.content.dto;
import com.project.module.content.entity.InteractiveNode;
import lombok.Data;
@Data public class InteractiveScenePlayVO {
    private Long id; private Long contentId; private String title; private String description;
    private String videoUrl; private Integer duration; private String sceneType;
    private Boolean ending; private InteractiveNode firstNode;
}
