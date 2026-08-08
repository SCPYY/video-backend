package com.project.module.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "创建订单请求")
public class CreateOrderRequest {

    @NotNull(message = "商品ID不能为空")
    @Schema(description = "商品ID", example = "1")
    private Long productId;

    @NotBlank(message = "币种不能为空")
    @Schema(description = "币种", example = "USD", allowableValues = {"USD", "EUR"})
    private String currency;

    @Schema(description = "支付方式", example = "PAYPAL", allowableValues = {"PAYPAL", "STRIPE"})
    private String paymentMethod;
}
