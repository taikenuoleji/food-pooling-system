package com.food.order.controller;

import com.food.common.result.Result;
import com.food.order.dto.OrderDetailVO;
import com.food.order.dto.OrderListItemVO;
import com.food.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/{orderId}")
    public Result<OrderDetailVO> getOrderDetail(@PathVariable String orderId) {
        return Result.success(orderService.getOrderDetail(orderId));
    }

    @GetMapping
    public Result<List<OrderListItemVO>> listMyOrders(@RequestHeader("X-User-Id") String userId,
                                                      @RequestParam(required = false) String status,
                                                      @RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "10") int size) {
        return Result.success(orderService.listMyOrders(userId, status, page, size));
    }

    @PostMapping("/{orderId}/cancel")
    public Result<Void> cancelOrder(@PathVariable String orderId,
                                    @RequestHeader("X-User-Id") String userId) {
        orderService.cancelOrder(orderId, userId);
        return Result.success();
    }

    /**
     * 内部接口：成团后创建订单
     */
    @PostMapping("/internal/create")
    public Result<String> createOrderInternal(@RequestBody com.food.order.dto.CreateOrderRequest request) {
        return Result.success(orderService.createOrder(request));
    }
}
