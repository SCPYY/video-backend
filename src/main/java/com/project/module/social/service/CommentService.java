package com.project.module.social.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.project.module.social.dto.CommentRequest;
import com.project.module.social.dto.CommentVO;
import com.project.module.social.dto.LikeResult;
import com.project.module.social.entity.Comment;

public interface CommentService extends IService<Comment> {

    /**
     * 发表评论/回复
     */
    CommentVO publishComment(CommentRequest request, Long userId, String ipAddress);

    /**
     * 获取评论列表（含前3条子回复）
     */
    Page<CommentVO> getCommentList(Long contentId, Long episodeId, Long userId,
                                   int page, int size, String sort);

    /**
     * 获取某条评论的子回复列表
     */
    Page<CommentVO> getSubReplies(Long commentId, Long userId, int page, int size);

    /**
     * 编辑评论（仅本人）
     */
    void editComment(Long commentId, Long userId, String content);

    /**
     * 删除评论（本人或管理员）
     */
    void deleteComment(Long commentId, Long userId, boolean isAdmin);

    /**
     * 点赞/取消点赞（幂等）
     */
    LikeResult toggleLike(Long commentId, Long userId);

    /**
     * 点踩/取消点踩（幂等）
     */
    LikeResult toggleDislike(Long commentId, Long userId);
}
