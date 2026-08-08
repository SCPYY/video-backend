package com.project.module.content.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "剧集列表项")
public class EpisodeVO {

    @Schema(description = "剧集ID", example = "1")
    private Long id;

    @Schema(description = "集数", example = "1")
    private Integer episodeNumber;

    @Schema(description = "标题", example = "第1集：命运的相遇")
    private String title;

    @Schema(description = "时长（秒）", example = "180")
    private Integer duration;

    @Schema(description = "影游互动配置（JSON），短剧为null")
    private Object interactiveConfig;

    @Schema(description = "是否免费：0-付费 1-免费", example = "1")
    private Integer isFree;

    @Schema(description = "排序", example = "1")
    private Integer sortOrder;
}
