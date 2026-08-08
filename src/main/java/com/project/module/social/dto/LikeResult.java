package com.project.module.social.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "点赞/点踩结果")
public class LikeResult {

    @Schema(description = "当前状态：true-已赞/已踩", example = "true")
    private boolean liked;

    @Schema(description = "操作后的计数", example = "6")
    private int count;
}
