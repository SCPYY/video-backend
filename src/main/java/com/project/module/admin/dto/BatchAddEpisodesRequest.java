package com.project.module.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.math.BigDecimal;

@Data
@Schema(description = "批量添加剧集请求")
public class BatchAddEpisodesRequest {

    @NotNull(message = "内容ID不能为空")
    @Schema(description = "内容ID", example = "1")
    private Long contentId;

    @NotNull(message = "剧集列表不能为空")
    @Schema(description = "剧集列表")
    private List<EpisodeItem> episodes;

    @Data
    @Schema(description = "单集信息")
    public static class EpisodeItem {

        @NotNull(message = "集数不能为空")
        @Schema(description = "集数/关卡编号", example = "1")
        private Integer episodeNumber;

        @Schema(description = "标题", example = "第一集：开端")
        private String title;

        @Schema(description = "视频URL")
        private String videoUrl;

        @Schema(description = "时长（秒）", example = "1800")
        private Integer duration;

        @Schema(description = "是否免费：0-付费 1-免费", example = "1")
        private Integer isFree;

        @Schema(description = "访问类型：1-免费集 2-付费集 3-会员免费", example = "2")
        private Integer accessType;

        @Schema(description = "付费集价格，单位：平台币", example = "10")
        private BigDecimal pricePlatformCoin;

        @Schema(description = "排序", example = "1")
        private Integer sortOrder;

        @Schema(description = "视频来源：1-本地上传 2-第三方URL", example = "1")
        private Integer sourceType;
    }
}
