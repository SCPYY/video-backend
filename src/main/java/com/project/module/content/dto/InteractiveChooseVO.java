package com.project.module.content.dto;
import lombok.Data;
@Data public class InteractiveChooseVO {
    private Long nextSceneId;
    private Long nextNodeId;
    private Boolean ending;
    private Long endingSceneId;
}
