package com.project.module.social.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "发送弹幕请求")
public class DanmakuRequest {

    @NotNull(message = "剧集ID不能为空")
    @Schema(description = "剧集ID", example = "1")
    private Long episodeId;

    @NotBlank(message = "弹幕内容不能为空")
    @Size(max = 200, message = "弹幕内容最多200字")
    @Schema(description = "弹幕内容（最多200字）", example = "前方高能！", maxLength = 200)
    private String content;

    @NotNull(message = "视频时间点不能为空")
    @Schema(description = "视频时间点（秒）", example = "30")
    private Integer videoTime;

    @Schema(description = "弹幕颜色（十六进制）", example = "#FF0000")
    private String color;

    @Schema(description = "弹幕位置：scroll/fixed_top/fixed_bottom", example = "scroll")
    private String position;
}
