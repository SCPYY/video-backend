package com.project.module.social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.project.module.social.entity.DanmakuLike;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DanmakuLikeMapper extends BaseMapper<DanmakuLike> {

    /** 查询用户对某条弹幕的点赞记录 */
    DanmakuLike selectByDanmakuAndUser(@Param("danmakuId") Long danmakuId,
                                       @Param("userId") Long userId);

    /** 批量查询用户对多条弹幕的点赞记录 */
    List<DanmakuLike> selectByUserAndDanmakuIds(@Param("userId") Long userId,
                                                 @Param("danmakuIds") List<Long> danmakuIds);
}
