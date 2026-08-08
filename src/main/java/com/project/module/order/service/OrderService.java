package com.project.module.order.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.project.module.order.dto.CreateOrderRequest;
import com.project.module.order.dto.OrderVO;
import com.project.module.order.entity.Order;

public interface OrderService extends IService<Order> {

    /**
     * 创建订单
     */
    OrderVO createOrder(Long userId, CreateOrderRequest req);

    /**
     * 订单详情
     */
    OrderVO getOrderDetail(Long userId, Long orderId);

    /**
     * 用户订单列表
     */
    Page<OrderVO> pageUserOrders(Long userId, Integer page, Integer size, Integer status);

    /**
     * 取消订单
     */
    void cancelOrder(Long userId, Long orderId);

    /**
     * 支付回调——更新订单状态
     */
    void markPaid(String gatewayTxId, String gatewayOrderId, Long orderId);

    /**
     * 根据订单号查询（Webhook 幂等用）
     */
    Order getByOrderNo(String orderNo);
}
