package com.project.module.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "钱包账户")
public class WalletVO {
    private Long id;
    private String currency;
    private BigDecimal availableBalance;
    private BigDecimal frozenBalance;
    private Integer status;
    private String statusReason;
    private LocalDateTime disabledAt;
    private Long disabledBy;
    private LocalDateTime updatedAt;
}
