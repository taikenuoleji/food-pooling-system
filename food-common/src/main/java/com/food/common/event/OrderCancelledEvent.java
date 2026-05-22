package com.food.common.event;

import lombok.Data;

import java.io.Serializable;

/**
 * 订单取消事件
 * 生产者: order-service
 * 消费者: payment-service (退款)
 */
@Data
public class OrderCancelledEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String orderId;
    private String poolId;
}
