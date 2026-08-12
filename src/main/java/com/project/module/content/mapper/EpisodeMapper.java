package com.project.module.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.project.module.content.entity.Episode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface EpisodeMapper extends BaseMapper<Episode> {

    @Update("UPDATE episodes SET play_count = COALESCE(play_count, 0) + 1 WHERE id = #{id}")
    int incrementPlayCount(@Param("id") Long id);

    /**
     * 批量插入剧集
     */
    int batchInsert(@Param("list") List<Episode> episodes);

    /**
     * 软删除指定内容的所有剧集
     */
    int softDeleteByContentId(@Param("contentId") Long contentId);

    /**
     * 统计某内容的剧集数量
     */
    int countByContentId(@Param("contentId") Long contentId);
}
