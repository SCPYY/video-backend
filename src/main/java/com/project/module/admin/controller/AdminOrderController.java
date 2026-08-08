package com.project.module.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.project.common.response.Result;
import com.project.module.order.dto.OrderVO;
import com.project.module.order.entity.Order;
import com.project.module.order.service.OrderService;
import com.project.module.product.entity.Product;
import com.project.module.product.mapper.ProductMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "管理后台-订单管理", description = "订单查询、状态管理")
@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;
    private final ProductMapper productMapper;

    @Operation(summary = "订单列表")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR','VIEWER')")
    public Result<Page<OrderVO>> list(
            @Parameter(description = "状态筛选") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") Integer size) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(status != null, Order::getStatus, status);
        wrapper.orderByDesc(Order::getId);

        Page<Order> orderPage = orderService.page(
                new Page<>(page != null ? page : 1, size != null ? size : 20), wrapper);

        // 批量查询商品名
        List<Long> productIds = orderPage.getRecords().stream()
                .map(Order::getProductId).distinct().collect(Collectors.toList());
        Map<Long, String> productNameMap = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Product::getName));

        List<OrderVO> records = orderPage.getRecords().stream().map(o -> {
            OrderVO vo = new OrderVO();
            BeanUtils.copyProperties(o, vo);
            vo.setProductName(productNameMap.get(o.getProductId()));
            return vo;
        }).collect(Collectors.toList());

        Page<OrderVO> result = new Page<>();
        BeanUtils.copyProperties(orderPage, result, "records");
        result.setRecords(records);
        return Result.ok(result);
    }

    @Operation(summary = "退款")
    @PutMapping("/{id}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> refund(@PathVariable Long id) {
        Order order = orderService.getById(id);
        if (order != null && order.getStatus() == 1) {
            order.setStatus(3); // 已退款
            orderService.updateById(order);
        }
        return Result.okMsg("已退款");
    }
}
