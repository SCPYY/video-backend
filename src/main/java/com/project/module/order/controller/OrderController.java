package com.project.module.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.project.common.response.Result;
import com.project.module.order.dto.CreateOrderRequest;
import com.project.module.order.dto.OrderVO;
import com.project.module.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "订单接口", description = "订单创建、查询、取消")
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    private Long getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }

    @Operation(summary = "创建订单")
    @PostMapping
    public Result<OrderVO> create(@Valid @RequestBody CreateOrderRequest req) {
        return Result.ok(orderService.createOrder(getUserId(), req));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{id}")
    public Result<OrderVO> detail(@PathVariable Long id) {
        return Result.ok(orderService.getOrderDetail(getUserId(), id));
    }

    @Operation(summary = "用户订单列表")
    @GetMapping
    public Result<Page<OrderVO>> list(
            @Parameter(description = "状态筛选：0-待支付 1-已支付 2-已取消 3-已退款") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer size) {
        return Result.ok(orderService.pageUserOrders(getUserId(), page, size, status));
    }

    @Operation(summary = "取消订单")
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        orderService.cancelOrder(getUserId(), id);
        return Result.okMsg("订单已取消");
    }
}
