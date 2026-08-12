package com.project.module.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Schema(description = "内容详情")
public class ContentDetailVO {

    @Schema(description = "内容ID", example = "1")
    private Long id;

    @Schema(description = "类型：1-短剧 2-影游", example = "1")
    private Integer type;

    @Schema(description = "标题", example = "霸道总裁爱上我")
    private String title;

    @Schema(description = "简介")
    private String description;

    @Schema(description = "封面图URL")
    private String coverUrl;

    @Schema(description = "分类")
    private String category;

    @Schema(description = "标签")
    private String tags;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "观看次数")
    private Long viewCount;
    private Long playCount;
    private Long uniqueViewCount;
    @Schema(description = "去重播放用户数")
    private Long playUserCount;
    private Long likeCount;
    private Long favoriteCount;
    private Long shareCount;
    private Long commentCount;
    private Long danmakuCount;

    @Schema(description = "排序权重")
    private Integer sortOrder;

    @Schema(description = "扩展属性（导演、演员、互动树等）")
    private Map<String, Object> extras;

    @Schema(description = "总集数")
    private Integer totalEpisodes;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
