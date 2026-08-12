package com.project.module.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.project.common.response.Result;
import com.project.module.order.dto.OrderVO;
import com.project.module.admin.dto.OrderCurrencyStatisticsVO;
import com.project.module.admin.mapper.OrderStatisticsMapper;
import com.project.module.order.entity.Order;
import com.project.module.order.service.OrderService;
import com.project.module.product.entity.Product;
import com.project.module.product.mapper.ProductMapper;
import com.project.module.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.stream.Collectors;

@Tag(name = "管理后台-订单管理", description = "订单查询和退款")
@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;
    private final ProductMapper productMapper;
    private final WalletService walletService;
    private final OrderStatisticsMapper orderStatisticsMapper;

    @Operation(summary = "订单金额币种统计", description = "已支付订单按 USD/EUR 分开统计，并按指定汇率统一换算。targetCurrency 支持 USD 或 EUR；eurToUsdRate 表示 1 EUR 可兑换多少 USD。")
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR','VIEWER')")
    public Result<OrderCurrencyStatisticsVO> statistics(
            @RequestParam(defaultValue = "USD") String targetCurrency,
            @RequestParam(defaultValue = "1.08") BigDecimal eurToUsdRate) {
        String target = targetCurrency.trim().toUpperCase();
        if (!("USD".equals(target) || "EUR".equals(target)) || eurToUsdRate.compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail(400, "统计币种或汇率参数不合法");
        }
        OrderCurrencyStatisticsVO vo = orderStatisticsMapper.paidStatistics();
        vo.setTargetCurrency(target);
        vo.setExchangeRate(eurToUsdRate);
        BigDecimal usd = vo.getUsdAmount();
        BigDecimal eur = vo.getEurAmount();
        BigDecimal converted = "USD".equals(target)
                ? usd.add(eur.multiply(eurToUsdRate))
                : usd.divide(eurToUsdRate, 2, RoundingMode.HALF_UP).add(eur);
        vo.setConvertedAmount(converted.setScale(2, RoundingMode.HALF_UP));
        return Result.ok(vo);
    }

    @Operation(summary = "订单列表")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR','VIEWER')")
    public Result<Page<OrderVO>> list(
            @Parameter(description = "状态筛选") @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(status != null, Order::getStatus, status);
        wrapper.orderByDesc(Order::getId);

        Page<Order> orderPage = orderService.page(new Page<>(page, size), wrapper);
        List<Long> productIds = orderPage.getRecords().stream()
                .map(Order::getProductId).distinct().collect(Collectors.toList());
        Map<Long, String> productNameMap = productIds.isEmpty()
                ? Collections.emptyMap()
                : productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Product::getName));

        List<OrderVO> records = orderPage.getRecords().stream().map(order -> {
            OrderVO vo = new OrderVO();
            BeanUtils.copyProperties(order, vo);
            vo.setProductName(productNameMap.get(order.getProductId()));
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
        walletService.refundOrder(id);
        return Result.okMsg("已退款");
    }
}
