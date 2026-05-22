package com.food.payment.mq;

import com.food.common.event.OrderCancelledEvent;
import com.food.payment.dto.RefundRequest;
import com.food.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 监听订单取消事件 → 退款
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = "order-cancelled-topic", consumerGroup = "payment-cancel-consumer")
public class OrderEventConsumer implements RocketMQListener<OrderCancelledEvent> {

    private final PaymentService paymentService;

    @Override
    public void onMessage(OrderCancelledEvent event) {
        log.info("收到订单取消事件，准备退款: orderId={}, poolId={}", event.getOrderId(), event.getPoolId());
        try {
            RefundRequest request = new RefundRequest();
            request.setPoolId(event.getPoolId());
            request.setReason("ORDER_CANCELLED");

            paymentService.refund(event.getPoolId(), request);
            log.info("订单取消退款完成: orderId={}", event.getOrderId());
        } catch (Exception e) {
            log.error("订单取消退款失败: orderId={}", event.getOrderId(), e);
            throw e;
        }
    }
}
