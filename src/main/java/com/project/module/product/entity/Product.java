package com.project.module.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("products")
public class Product {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer type;         // 1-单集解锁 2-全集解锁 3-会员
    private Long contentId;
    private Long episodeId;
    private String name;
    private BigDecimal priceUsd;
    private BigDecimal priceEur;
    private Integer durationDays; // 会员有效期天数
    private Integer status;       // 0-下架 1-上架

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
