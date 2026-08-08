package com.project.module.social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.project.module.social.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    /**
     * 分页查询一级评论（含用户信息）
     */
    List<Comment> selectRootComments(@Param("contentId") Long contentId,
                                      @Param("episodeId") Long episodeId,
                                      @Param("offset") int offset,
                                      @Param("limit") int limit,
                                      @Param("sort") String sort);

    /**
     * 统计一级评论数量
     */
    long countRootComments(@Param("contentId") Long contentId,
                           @Param("episodeId") Long episodeId);

    /**
     * 批量查询根评论下的子回复（每个根评论取前N条）
     */
    List<Comment> selectSubRepliesByRootIds(@Param("rootIds") List<Long> rootIds,
                                             @Param("limit") int limit);

    /**
     * 回复数+1
     */
    void incrementReplyCount(@Param("commentId") Long commentId);

    /**
     * 点赞数+1
     */
    void incrementLikeCount(@Param("commentId") Long commentId);

    /**
     * 点赞数-1
     */
    void decrementLikeCount(@Param("commentId") Long commentId);

    /**
     * 点踩数+1
     */
    void incrementDislikeCount(@Param("commentId") Long commentId);

    /**
     * 点踩数-1
     */
    void decrementDislikeCount(@Param("commentId") Long commentId);
}
