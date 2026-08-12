package com.project.module.admin.mapper;

import com.project.module.admin.dto.WalletCurrencyStatisticsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WalletStatisticsMapper {
    @Select("SELECT " +
            "SUM(currency='PLATFORM_COIN') platform_coin_wallet_count, " +
            "SUM(status=1) normal_wallet_count, SUM(status=2) frozen_wallet_count, SUM(status=3) closed_wallet_count, " +
            "COALESCE(SUM(available_balance),0) platform_coin_available_balance, " +
            "COALESCE(SUM(frozen_balance),0) platform_coin_frozen_balance " +
            "FROM user_wallets")
    WalletCurrencyStatisticsVO statistics();
}
