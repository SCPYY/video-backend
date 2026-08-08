package com.project.module.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "调整剧集排序请求")
public class SortEpisodesRequest {

    @NotNull(message = "内容ID不能为空")
    @Schema(description = "内容ID", example = "1")
    private Long contentId;

    @NotNull(message = "排序列表不能为空")
    @Schema(description = "排序列表")
    private List<EpisodeOrder> episodeOrders;

    @Data
    @Schema(description = "排序项")
    public static class EpisodeOrder {

        @NotNull(message = "剧集ID不能为空")
        @Schema(description = "剧集ID", example = "1")
        private Long episodeId;

        @NotNull(message = "排序值不能为空")
        @Schema(description = "排序值", example = "1")
        private Integer sortOrder;
    }
}
