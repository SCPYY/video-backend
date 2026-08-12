package com.project.module.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.project.module.content.dto.HotTagVO;
import com.project.module.content.entity.SearchLog;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface SearchLogMapper extends BaseMapper<SearchLog> {

    @Select("SELECT normalized_keyword AS tag, COUNT(*) AS search_count " +
            "FROM search_logs WHERE created_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY) " +
            "GROUP BY normalized_keyword ORDER BY search_count DESC, MAX(created_at) DESC LIMIT #{limit}")
    List<HotTagVO> selectHotTags(@Param("days") int days, @Param("limit") int limit);

}
