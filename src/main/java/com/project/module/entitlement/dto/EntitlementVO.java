package com.project.module.entitlement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "用户权益")
public class EntitlementVO {

    @Schema(description = "权益ID", example = "3")
    private Long id;

    @Schema(description = "用户ID", example = "3")
    private Long userId;

    @Schema(description = "类型：1-内容解锁 2-会员", example = "2")
    private Integer type;

    @Schema(description = "内容ID（解锁类权益）")
    private Long contentId;

    @Schema(description = "内容标题", example = "霸道总裁爱上我")
    private String contentTitle;

    @Schema(description = "剧集ID（单集解锁时）")
    private Long episodeId;

    @Schema(description = "剧集编号", example = "2")
    private Integer episodeNumber;

    @Schema(description = "到期时间（NULL=永久）")
    private LocalDateTime expireTime;

    @Schema(description = "是否已过期", example = "false")
    private Boolean isExpired;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
