package com.project.module.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.project.common.exception.BusinessException;
import com.project.common.exception.ErrorCode;
import com.project.module.order.dto.CreateOrderRequest;
import com.project.module.order.dto.OrderVO;
import com.project.module.order.entity.Order;
import com.project.module.order.mapper.OrderMapper;
import com.project.module.order.service.OrderService;
import com.project.module.product.entity.Product;
import com.project.module.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private final ProductMapper productMapper;

    @Override
    @Transactional
    public OrderVO createOrder(Long userId, CreateOrderRequest req) {
        // 1. 校验商品
        Product product = productMapper.selectById(req.getProductId());
        if (product == null || product.getStatus() == 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "商品不存在或已下架");
        }

        // 2. 确定金额和币种
        String currency = req.getCurrency().toUpperCase();
        BigDecimal amount;
        if ("EUR".equals(currency)) {
            amount = product.getPriceEur();
        } else {
            amount = product.getPriceUsd();
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "商品价格配置异常");
        }

        // 3. 检查重复下单（同一用户+同一商品+待支付）
        long dupCount = count(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(Order::getProductId, req.getProductId())
                .eq(Order::getStatus, 0));
        if (dupCount > 0) {
            throw new BusinessException(ErrorCode.DUPLICATE_ORDER);
        }

        // 4. 创建订单
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setProductId(req.getProductId());
        order.setAmount(amount);
        order.setCurrency(currency);
        order.setPaymentMethod(req.getPaymentMethod() != null ? req.getPaymentMethod().toUpperCase() : null);
        order.setStatus(0); // 待支付
        order.setExpiredAt(LocalDateTime.now().plusMinutes(30)); // 30分钟过期
        save(order);

        log.info("订单创建成功: orderNo={}, userId={}, amount={} {}", order.getOrderNo(), userId, amount, currency);

        return toVO(order, product.getName());
    }

    @Override
    public OrderVO getOrderDetail(Long userId, Long orderId) {
        Order order = getById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        Product product = productMapper.selectById(order.getProductId());
        return toVO(order, product != null ? product.getName() : null);
    }

    @Override
    public Page<OrderVO> pageUserOrders(Long userId, Integer pageNum, Integer size, Integer status) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId);
        wrapper.eq(status != null, Order::getStatus, status);
        wrapper.orderByDesc(Order::getId);

        Page<Order> page = page(new Page<>(pageNum != null ? pageNum : 1, size != null ? size : 10), wrapper);

        // 批量获取商品名称
        List<Long> productIds = page.getRecords().stream().map(Order::getProductId).distinct().collect(Collectors.toList());
        Map<Long, String> productNameMap = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Product::getName));

        List<OrderVO> records = page.getRecords().stream()
                .map(o -> toVO(o, productNameMap.get(o.getProductId())))
                .collect(Collectors.toList());

        Page<OrderVO> result = new Page<>();
        BeanUtils.copyProperties(page, result, "records");
        result.setRecords(records);
        return result;
    }

    @Override
    @Transactional
    public void cancelOrder(Long userId, Long orderId) {
        Order order = getById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != 0) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_CANCEL);
        }
        order.setStatus(2); // 已取消
        updateById(order);
    }

    @Override
    @Transactional
    public void markPaid(String gatewayTxId, String gatewayOrderId, Long orderId) {
        Order order = getById(orderId);
        if (order == null) {
            log.error("订单不存在: id={}", orderId);
            return;
        }
        order.setStatus(1); // 已支付
        order.setGatewayTxId(gatewayTxId);
        order.setGatewayOrderId(gatewayOrderId);
        order.setPaidAt(LocalDateTime.now());
        updateById(order);
        log.info("订单支付成功: orderNo={}", order.getOrderNo());
    }

    @Override
    public Order getByOrderNo(String orderNo) {
        return getOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
    }

    // --- 私有方法 ---

    private String generateOrderNo() {
        long ts = System.currentTimeMillis();
        int rand = (int) (Math.random() * 9000) + 1000;
        return "ORD" + ts + rand;
    }

    private OrderVO toVO(Order order, String productName) {
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);
        vo.setProductName(productName);
        return vo;
    }
}
