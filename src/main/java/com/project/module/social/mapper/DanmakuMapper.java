package com.project.module.social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.project.module.social.entity.Danmaku;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DanmakuMapper extends BaseMapper<Danmaku> {

    /** 按时间段查询弹幕 */
    List<Danmaku> selectByTimeRange(@Param("episodeId") Long episodeId,
                                    @Param("startTime") Integer startTime,
                                    @Param("endTime") Integer endTime);

    /** 增加点赞数 */
    int incrementLikeCount(@Param("id") Long id);
}
