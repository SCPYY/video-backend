package com.project.module.admin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Data
@Schema(description = "管理端商品创建/更新请求")
public class ProductSaveRequest {
    @Schema(description = "商品类型：1-单集解锁，2-全集解锁，3-会员")
    @NotNull private Integer type;
    @Schema(description = "关联内容 ID；会员商品为空") private Long contentId;
    @Schema(description = "关联剧集 ID；单集商品必填") private Long episodeId;
    @Schema(description = "商品名称") @NotBlank private String name;
    @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) private BigDecimal priceUsd;
    @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) private BigDecimal priceEur;
    @Schema(description = "会员有效天数，非会员商品为空") private Integer durationDays;
    @Schema(description = "状态：0-下架，1-上架") @NotNull private Integer status;
}
