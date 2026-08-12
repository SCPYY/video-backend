package com.project.module.admin.mapper;

import com.project.module.admin.dto.WalletTransactionStatisticsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WalletTransactionStatisticsMapper {
    @Select("SELECT COUNT(*) total_count, " +
            "COALESCE(SUM(CASE WHEN currency='USD' AND direction='IN' THEN amount ELSE 0 END),0) usd_in_amount, " +
            "COALESCE(SUM(CASE WHEN currency='USD' AND direction='OUT' THEN amount ELSE 0 END),0) usd_out_amount, " +
            "COALESCE(SUM(CASE WHEN currency='EUR' AND direction='IN' THEN amount ELSE 0 END),0) eur_in_amount, " +
            "COALESCE(SUM(CASE WHEN currency='EUR' AND direction='OUT' THEN amount ELSE 0 END),0) eur_out_amount, " +
            "COALESCE(SUM(CASE WHEN type='RECHARGE' THEN amount ELSE 0 END),0) recharge_amount, " +
            "COALESCE(SUM(CASE WHEN type='ADJUSTMENT' THEN amount ELSE 0 END),0) adjustment_amount, " +
            "COALESCE(SUM(CASE WHEN type='PAYMENT' THEN amount ELSE 0 END),0) payment_amount, " +
            "COALESCE(SUM(CASE WHEN type='REFUND' THEN amount ELSE 0 END),0) refund_amount " +
            "FROM wallet_transactions")
    WalletTransactionStatisticsVO statistics();
}
