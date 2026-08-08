package com.project.module.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "订单信息")
public class OrderVO {

    @Schema(description = "订单ID", example = "1")
    private Long id;

    @Schema(description = "订单号", example = "ORD17861020809198268")
    private String orderNo;

    @Schema(description = "用户ID", example = "3")
    private Long userId;

    @Schema(description = "商品ID", example = "1")
    private Long productId;

    @Schema(description = "商品名称", example = "第2集解锁")
    private String productName;

    @Schema(description = "支付金额", example = "0.99")
    private BigDecimal amount;

    @Schema(description = "币种", example = "USD")
    private String currency;

    @Schema(description = "支付方式：PAYPAL / STRIPE", example = "PAYPAL")
    private String paymentMethod;

    @Schema(description = "状态：0-待支付 1-已支付 2-已取消 3-已退款", example = "0")
    private Integer status;

    @Schema(description = "支付网关订单ID")
    private String gatewayOrderId;

    @Schema(description = "支付时间")
    private LocalDateTime paidAt;

    @Schema(description = "订单过期时间")
    private LocalDateTime expiredAt;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
