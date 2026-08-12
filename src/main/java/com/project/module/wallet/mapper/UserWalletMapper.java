package com.project.module.wallet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.project.module.wallet.entity.UserWallet;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface UserWalletMapper extends BaseMapper<UserWallet> {

    @Insert("INSERT IGNORE INTO user_wallets " +
            "(user_id, currency, available_balance, frozen_balance, status, version, created_at, updated_at) " +
            "VALUES (#{userId}, #{currency}, 0, 0, 1, 0, NOW(), NOW())")
    int ensureWallet(@Param("userId") Long userId, @Param("currency") String currency);

    @Select("SELECT * FROM user_wallets WHERE user_id = #{userId} AND currency = #{currency} FOR UPDATE")
    UserWallet selectForUpdate(@Param("userId") Long userId, @Param("currency") String currency);

    @Update("UPDATE user_wallets SET available_balance = #{balance}, version = version + 1, updated_at = NOW() " +
            "WHERE id = #{walletId}")
    int updateAvailableBalance(@Param("walletId") Long walletId, @Param("balance") BigDecimal balance);
}
