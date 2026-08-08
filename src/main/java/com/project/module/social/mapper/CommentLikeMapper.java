package com.project.module.social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.project.module.social.entity.CommentLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CommentLikeMapper extends BaseMapper<CommentLike> {

    /**
     * 根据评论ID和用户ID查询互动记录
     */
    CommentLike selectByCommentAndUser(@Param("commentId") Long commentId,
                                        @Param("userId") Long userId);

    /**
     * 批量查询用户对多个评论的点赞状态
     */
    List<CommentLike> selectByUserAndCommentIds(@Param("userId") Long userId,
                                                  @Param("commentIds") List<Long> commentIds);
}
