package com.project.module.social.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "弹幕响应")
public class DanmakuVO {

    @Schema(description = "弹幕ID", example = "1")
    private Long id;

    @Schema(description = "剧集ID", example = "1")
    private Long episodeId;

    @Schema(description = "用户ID", example = "3")
    private Long userId;

    @Schema(description = "用户昵称", example = "弹幕君")
    private String nickname;

    @Schema(description = "用户头像")
    private String avatarUrl;

    @Schema(description = "弹幕内容", example = "前方高能！")
    private String content;

    @Schema(description = "视频时间点（秒）", example = "30")
    private Integer videoTime;

    @Schema(description = "弹幕颜色", example = "#FF0000")
    private String color;

    @Schema(description = "弹幕位置", example = "scroll")
    private String position;

    @Schema(description = "点赞数", example = "15")
    private Integer likeCount;

    @Schema(description = "是否已点赞")
    private Boolean liked;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
