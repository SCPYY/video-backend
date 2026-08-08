package com.project.module.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.project.common.exception.BusinessException;
import com.project.common.exception.ErrorCode;
import com.project.common.response.Result;
import com.project.module.product.entity.Product;
import com.project.module.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "商品接口", description = "商品列表、详情查询")
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "商品列表（分页）")
    @GetMapping
    public Result<Page<Product>> list(
            @Parameter(description = "类型：1-单集解锁 2-全集解锁 3-会员") @RequestParam(required = false) Integer type,
            @Parameter(description = "按内容ID筛选") @RequestParam(required = false) Long contentId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") Integer size) {
        return Result.ok(productService.pageProducts(type, contentId, page, size));
    }

    @Operation(summary = "商品详情")
    @GetMapping("/{id}")
    public Result<Product> detail(@PathVariable Long id) {
        Product product = productService.getById(id);
        if (product == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "商品不存在");
        }
        return Result.ok(product);
    }

    @Operation(summary = "某内容的可购商品")
    @GetMapping("/by-content/{contentId}")
    public Result<List<Product>> byContent(@PathVariable Long contentId) {
        return Result.ok(productService.getByContentId(contentId));
    }

    @Operation(summary = "会员套餐列表")
    @GetMapping("/memberships")
    public Result<List<Product>> memberships() {
        return Result.ok(productService.getMembershipPlans());
    }
}
