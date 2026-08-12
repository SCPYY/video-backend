package com.project.module.entitlement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "我的已购内容权益")
public class MyEntitlementVO {

    private Long entitlementId;
    private Long contentId;

    @Schema(description = "内容类型：1-短剧 2-影游")
    private Integer contentType;

    private String title;
    private String description;
    private String coverUrl;
    private String category;
    private String tags;

    @Schema(description = "解锁范围：FULL_CONTENT-整部 SINGLE_EPISODE-单集")
    private String accessScope;

    private Long episodeId;
    private Integer episodeNumber;
    private String episodeTitle;

    @Schema(description = "到期时间，NULL 表示永久")
    private LocalDateTime expireTime;
    private Boolean permanent;
    private Boolean expired;
    private LocalDateTime acquiredAt;
}
