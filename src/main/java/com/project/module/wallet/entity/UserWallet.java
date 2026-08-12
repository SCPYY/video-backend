package com.project.module.wallet.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("user_wallets")
public class UserWallet {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String currency;
    private BigDecimal availableBalance;
    private BigDecimal frozenBalance;
    private Integer status;
    private String statusReason;
    private LocalDateTime disabledAt;
    private Long disabledBy;
    private Integer version;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
