package com.project.module.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("orders")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;
    private Long userId;
    private Long productId;
    private BigDecimal amount;
    private String currency;      // USD / EUR
    private String paymentMethod; // PAYPAL / STRIPE
    private Integer status;       // 0-待支付 1-已支付 2-已取消 3-已退款
    private String gatewayOrderId;
    private String gatewayTxId;
    private LocalDateTime paidAt;
    private LocalDateTime expiredAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
