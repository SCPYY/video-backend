package com.project.module.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.project.common.response.Result;
import com.project.module.admin.dto.ProductSaveRequest;
import com.project.module.product.entity.Product;
import com.project.module.product.mapper.ProductMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理后台-商品管理", description = "商品的查询、创建、编辑、上下架和删除")
@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
public class AdminProductController {
    private final ProductMapper productMapper;

    @GetMapping
    @Operation(summary = "商品分页列表", description = "按商品类型、关联内容和上下架状态筛选商品。类型：1-单集解锁，2-全集解锁，3-会员；状态：0-下架，1-上架。")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR','VIEWER')")
    public Result<Page<Product>> list(@Parameter(description = "商品类型：1-单集解锁，2-全集解锁，3-会员") @RequestParam(required = false) Integer type,
                                      @Parameter(description = "关联内容 ID") @RequestParam(required = false) Long contentId,
                                      @Parameter(description = "状态：0-下架，1-上架") @RequestParam(required = false) Integer status,
                                      @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") Integer page,
                                      @Parameter(description = "每页条数，最大 100") @RequestParam(defaultValue = "20") Integer size) {
        LambdaQueryWrapper<Product> w = new LambdaQueryWrapper<Product>()
                .eq(type != null, Product::getType, type)
                .eq(contentId != null, Product::getContentId, contentId)
                .eq(status != null, Product::getStatus, status)
                .orderByDesc(Product::getId);
        return Result.ok(productMapper.selectPage(new Page<>(Math.max(1, page), Math.min(100, Math.max(1, size))), w));
    }

    @PostMapping
    @Operation(summary = "创建商品", description = "创建单集、全集或会员商品。ADMIN 和 EDITOR 可操作。")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public Result<Product> create(@Valid @RequestBody ProductSaveRequest request) {
        Product product = new Product();
        copy(request, product);
        productMapper.insert(product);
        return Result.ok(product);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新商品", description = "更新商品名称、关联内容、价格、会员期限和上下架状态。")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public Result<Product> update(@PathVariable Long id, @Valid @RequestBody ProductSaveRequest request) {
        Product product = productMapper.selectById(id);
        if (product == null) return Result.ok();
        copy(request, product);
        productMapper.updateById(product);
        return Result.ok(product);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除商品", description = "删除商品记录，仅 ADMIN 可操作；已产生订单的商品建议改为下架。")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        productMapper.deleteById(id);
        return Result.okMsg("删除成功");
    }

    @PutMapping("/{id}/toggle-status")
    @Operation(summary = "切换商品状态", description = "在上架和下架之间切换商品状态。")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        Product product = productMapper.selectById(id);
        if (product != null) {
            product.setStatus(product.getStatus() == 1 ? 0 : 1);
            productMapper.updateById(product);
        }
        return Result.okMsg("状态已切换");
    }

    private void copy(ProductSaveRequest r, Product p) {
        p.setType(r.getType()); p.setContentId(r.getContentId()); p.setEpisodeId(r.getEpisodeId());
        p.setName(r.getName()); p.setPriceUsd(r.getPriceUsd()); p.setPriceEur(r.getPriceEur());
        p.setDurationDays(r.getDurationDays()); p.setStatus(r.getStatus());
    }
}
