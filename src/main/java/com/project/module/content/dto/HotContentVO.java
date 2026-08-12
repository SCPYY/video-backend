package com.project.module.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "按浏览量排序的热门内容")
public class HotContentVO {
    private Integer rank;
    private Long id;
    @Schema(description = "类型：1-短剧 2-影游")
    private Integer type;
    private String title;
    private String coverUrl;
    private String description;
    private String category;
    private String tags;
    private Long viewCount;
    private Integer totalEpisodes;
    private Integer freeEpisodes;
    private LocalDateTime createdAt;
}
