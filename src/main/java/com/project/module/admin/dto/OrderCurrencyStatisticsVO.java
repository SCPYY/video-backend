package com.project.module.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Schema(description = "订单币种统计")
public class OrderCurrencyStatisticsVO {
    private Long paidOrderCount;
    private BigDecimal usdAmount;
    private BigDecimal eurAmount;
    private String targetCurrency;
    private BigDecimal exchangeRate;
    private BigDecimal convertedAmount;
}
