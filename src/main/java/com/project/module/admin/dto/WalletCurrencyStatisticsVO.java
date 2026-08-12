package com.project.module.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Schema(description = "钱包币种统计")
public class WalletCurrencyStatisticsVO {
    private Long platformCoinWalletCount;
    private Long normalWalletCount;
    private Long frozenWalletCount;
    private Long closedWalletCount;
    private BigDecimal platformCoinAvailableBalance;
    private BigDecimal platformCoinFrozenBalance;
}
