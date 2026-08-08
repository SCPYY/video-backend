package com.project.module.social.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "评论/回复请求")
public class CommentRequest {

    @Schema(description = "内容ID", example = "1")
    @NotNull(message = "内容ID不能为空")
    private Long contentId;

    @Schema(description = "剧集ID，null表示整剧评论", example = "1")
    private Long episodeId;

    @Schema(description = "父评论ID，null表示一级评论", example = "1")
    private Long parentId;

    @Schema(description = "评论内容", example = "这部剧太好看了！")
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 500, message = "评论内容最多500字")
    private String content;
}
