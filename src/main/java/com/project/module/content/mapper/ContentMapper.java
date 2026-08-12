package com.project.module.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.project.module.content.entity.Content;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ContentMapper extends BaseMapper<Content> {

    @Update("UPDATE contents SET play_count = COALESCE(play_count, 0) + 1 WHERE id = #{id}")
    int incrementPlayCount(@Param("id") Long id);

    @Update("UPDATE contents SET view_count = COALESCE(view_count, 0) + #{delta} WHERE id = #{id}")
    int addViewCount(@Param("id") Long id, @Param("delta") Long delta);

    @Update("UPDATE contents SET play_user_count = COALESCE(play_user_count, 0) + #{delta} WHERE id = #{id}")
    int addPlayUserCount(@Param("id") Long id, @Param("delta") Long delta);
}
