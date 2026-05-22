package com.food.order.service;

import com.food.order.dto.CreateOrderRequest;
import com.food.order.dto.OrderDetailVO;
import com.food.order.dto.OrderListItemVO;

import java.util.List;

public interface OrderService {

    String createOrder(CreateOrderRequest request);

    OrderDetailVO getOrderDetail(String orderId);

    List<OrderListItemVO> listMyOrders(String userId, String status, int page, int size);

    void cancelOrder(String orderId, String userId);
}
