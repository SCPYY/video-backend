package com.project.module.content.dto;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
@Data public class InteractiveChooseRequest {
    @NotNull private Long optionId;
    private Long sceneId;
    private Integer position;
}
