package com.project.module.wallet.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WalletAdjustRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "钱包类型不能为空")
    @Pattern(regexp = "PLATFORM_COIN", message = "钱包仅支持平台币")
    private String currency;

    @NotNull(message = "调整金额不能为空")
    @DecimalMin(value = "0.01", message = "调整金额必须大于0")
    @Digits(integer = 16, fraction = 2, message = "金额最多保留两位小数")
    private BigDecimal amount;

    @NotBlank(message = "调整方向不能为空")
    @Pattern(regexp = "IN|OUT", message = "调整方向仅支持IN或OUT")
    private String direction;

    @NotBlank(message = "流水类型不能为空")
    @Pattern(regexp = "RECHARGE|ADJUSTMENT", message = "流水类型仅支持RECHARGE或ADJUSTMENT")
    private String transactionType;

    @NotBlank(message = "请求号不能为空")
    @Size(max = 64, message = "请求号不能超过64个字符")
    private String requestNo;

    @NotBlank(message = "调整原因不能为空")
    @Size(max = 255, message = "调整原因不能超过255个字符")
    private String remark;
}
