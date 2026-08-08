package com.project.module.social.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "评论信息")
public class CommentVO {

    @Schema(description = "评论ID", example = "1")
    private Long id;

    @Schema(description = "内容ID", example = "1")
    private Long contentId;

    @Schema(description = "剧集ID", example = "1")
    private Long episodeId;

    @Schema(description = "父评论ID，0表示一级评论", example = "0")
    private Long parentId;

    @Schema(description = "根评论ID", example = "0")
    private Long rootId;

    @Schema(description = "评论用户ID", example = "1")
    private Long userId;

    @Schema(description = "用户昵称", example = "testuser")
    private String nickname;

    @Schema(description = "用户头像URL")
    private String avatarUrl;

    @Schema(description = "回复的目标用户昵称")
    private String replyToNickname;

    @Schema(description = "评论内容", example = "这部剧太好看了！")
    private String content;

    @Schema(description = "点赞数", example = "5")
    private Integer likeCount;

    @Schema(description = "点踩数", example = "0")
    private Integer dislikeCount;

    @Schema(description = "子回复数量", example = "3")
    private Integer replyCount;

    @Schema(description = "当前用户是否已点赞")
    private Boolean liked;

    @Schema(description = "当前用户是否已点踩")
    private Boolean disliked;

    @Schema(description = "前3条子回复")
    private List<CommentVO> subReplies;

    @Schema(description = "是否有更多子回复")
    private Boolean hasMoreSubReplies;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
