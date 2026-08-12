package com.project.module.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "管理员人工发放权益请求")
public class GrantEntitlementRequest {
    @Schema(description = "接收权益的用户 ID") @NotNull private Long userId;
    @Schema(description = "用于确定权益类型和内容范围的商品 ID") @NotNull private Long productId;
}
