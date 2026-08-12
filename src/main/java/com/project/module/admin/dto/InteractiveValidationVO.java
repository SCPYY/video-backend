package com.project.module.admin.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;
@Data @Schema(description="影游剧情校验结果")
public class InteractiveValidationVO {
    private boolean valid;
    private int sceneCount;
    private int nodeCount;
    private int optionCount;
    private List<String> errors = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
}
