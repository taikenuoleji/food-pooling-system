package com.food.order.mq;

import com.food.common.event.OrderCancelledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * 订单事件 MQ 生产者
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private final RocketMQTemplate rocketMQTemplate;

    private static final String ORDER_CANCELLED_TOPIC = "order-cancelled-topic";

    public void sendOrderCancelled(OrderCancelledEvent event) {
        log.info("发送订单取消事件: orderId={}, poolId={}", event.getOrderId(), event.getPoolId());
        rocketMQTemplate.send(ORDER_CANCELLED_TOPIC,
                MessageBuilder.withPayload(event).build());
    }
}
