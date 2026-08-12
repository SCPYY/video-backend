package com.project.module.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "钱包资金流水")
public class WalletTransactionVO {
    private Long id;
    private String transactionNo;
    private String currency;
    private String type;
    private String direction;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String relatedType;
    private String relatedId;
    private String remark;
    private LocalDateTime createdAt;
}
