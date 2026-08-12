package com.project.module.admin.mapper;

import com.project.module.admin.dto.DashboardSummaryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DashboardMapper {
    @Select("SELECT " +
            "(SELECT COUNT(*) FROM sys_user) AS total_users, " +
            "(SELECT COUNT(*) FROM contents WHERE status=1) AS online_contents, " +
            "(SELECT COUNT(*) FROM contents WHERE status=1 AND type=1) AS drama_contents, " +
            "(SELECT COUNT(*) FROM contents WHERE status=1 AND type=2) AS game_contents, " +
            "(SELECT COUNT(*) FROM orders) AS total_orders, " +
            "(SELECT COUNT(*) FROM orders WHERE status=1) AS paid_orders, " +
            "(SELECT COUNT(*) FROM orders WHERE status=0) AS pending_orders, " +
            "(SELECT COALESCE(SUM(amount),0) FROM orders WHERE status=1) AS paid_amount, " +
            "(SELECT COUNT(*) FROM comment_reports WHERE status=0) AS pending_reports, " +
            "(SELECT COUNT(*) FROM search_logs WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)) AS search_count")
    DashboardSummaryVO summary();
}
