package com.project.module.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Schema(description = "钱包流水统计")
public class WalletTransactionStatisticsVO {
    private Long totalCount;
    private BigDecimal usdInAmount;
    private BigDecimal usdOutAmount;
    private BigDecimal eurInAmount;
    private BigDecimal eurOutAmount;
    private BigDecimal rechargeAmount;
    private BigDecimal adjustmentAmount;
    private BigDecimal paymentAmount;
    private BigDecimal refundAmount;
}
