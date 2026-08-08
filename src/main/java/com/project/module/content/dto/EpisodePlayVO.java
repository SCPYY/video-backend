package com.project.module.content.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "剧集播放信息（含鉴权结果）")
public class EpisodePlayVO {

    @Schema(description = "剧集ID", example = "2")
    private Long id;

    @Schema(description = "所属内容ID", example = "1")
    private Long contentId;

    @Schema(description = "集数", example = "2")
    private Integer episodeNumber;

    @Schema(description = "标题", example = "第2集：误会重重")
    private String title;

    @Schema(description = "视频URL（有权限时才返回）")
    private String videoUrl;

    @Schema(description = "时长（秒）", example = "195")
    private Integer duration;

    @Schema(description = "影游互动配置（JSON），短剧为null")
    private Object interactiveConfig;

    @Schema(description = "是否免费", example = "0")
    private Integer isFree;

    @Schema(description = "是否有播放权限", example = "true")
    private Boolean hasAccess;

    @Schema(description = "无权限时推荐的购买商品ID", example = "1")
    private Long suggestedProductId;
}
