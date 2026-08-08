package com.project.module.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "内容列表项")
public class ContentListItemVO {

    @Schema(description = "内容ID", example = "1")
    private Long id;

    @Schema(description = "类型：1-短剧 2-影游", example = "1")
    private Integer type;

    @Schema(description = "标题", example = "霸道总裁爱上我")
    private String title;

    @Schema(description = "封面图URL")
    private String coverUrl;

    @Schema(description = "分类", example = "爱情")
    private String category;

    @Schema(description = "标签（逗号分隔）", example = "短剧,爱情,都市")
    private String tags;

    @Schema(description = "状态：0-下架 1-上架", example = "1")
    private Integer status;

    @Schema(description = "观看次数", example = "15000")
    private Long viewCount;

    @Schema(description = "总集数", example = "3")
    private Integer totalEpisodes;

    @Schema(description = "免费集数", example = "1")
    private Integer freeEpisodes;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
