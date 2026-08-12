package com.project.module.admin.mapper;

import com.project.module.admin.dto.OrderCurrencyStatisticsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OrderStatisticsMapper {
    @Select("SELECT COUNT(*) AS paid_order_count, " +
            "COALESCE(SUM(CASE WHEN currency='USD' THEN amount ELSE 0 END),0) AS usd_amount, " +
            "COALESCE(SUM(CASE WHEN currency='EUR' THEN amount ELSE 0 END),0) AS eur_amount " +
            "FROM orders WHERE status=1")
    OrderCurrencyStatisticsVO paidStatistics();
}
