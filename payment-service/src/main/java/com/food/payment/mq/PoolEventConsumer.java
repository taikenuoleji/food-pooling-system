package com.food.payment.mq;

import com.food.common.event.PoolFormedEvent;
import com.food.common.event.PoolDissolvedEvent;
import com.food.payment.dto.ChargeRequest;
import com.food.payment.dto.RefundRequest;
import com.food.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 监听拼单事件
 * pool-formed-topic → 扣款
 * pool-dissolved-topic → 退款
 */
public class PoolEventConsumer {

    @Slf4j
    @Component
    @RequiredArgsConstructor
    @RocketMQMessageListener(topic = "pool-formed-topic", consumerGroup = "payment-formed-consumer")
    public static class FormedListener implements RocketMQListener<PoolFormedEvent> {

        private final PaymentService paymentService;

        @Override
        public void onMessage(PoolFormedEvent event) {
            log.info("收到成团事件，准备扣款: poolId={}", event.getPoolId());
            try {
                ChargeRequest request = new ChargeRequest();
                request.setPoolId(event.getPoolId());
                request.setOrderId(""); // 订单ID由 order-service 生成后回填

                List<ChargeRequest.ChargeItem> charges = new ArrayList<>();
                for (PoolFormedEvent.Participant p : event.getParticipants()) {
                    ChargeRequest.ChargeItem item = new ChargeRequest.ChargeItem();
                    item.setUserId(p.getUserId());
                    item.setFoodAmount(p.getFoodAmount());
                    item.setDeliveryShare(p.getDeliveryShare());
                    item.setPackagingShare(p.getPackagingShare());
                    item.setTotalAmount(p.getTotalAmount());
                    charges.add(item);
                }
                request.setCharges(charges);

                paymentService.charge(event.getPoolId(), request);
                log.info("成团扣款完成: poolId={}", event.getPoolId());
            } catch (Exception e) {
                log.error("成团扣款失败: poolId={}", event.getPoolId(), e);
                throw e;
            }
        }
    }

    @Slf4j
    @Component
    @RequiredArgsConstructor
    @RocketMQMessageListener(topic = "pool-dissolved-topic", consumerGroup = "payment-dissolved-consumer")
    public static class DissolvedListener implements RocketMQListener<PoolDissolvedEvent> {

        private final PaymentService paymentService;

        @Override
        public void onMessage(PoolDissolvedEvent event) {
            log.info("收到解散事件，准备退款: poolId={}, reason={}", event.getPoolId(), event.getReason());
            try {
                RefundRequest request = new RefundRequest();
                request.setPoolId(event.getPoolId());
                request.setReason(event.getReason());

                paymentService.refund(event.getPoolId(), request);
                log.info("解散退款完成: poolId={}", event.getPoolId());
            } catch (Exception e) {
                log.error("解散退款失败: poolId={}", event.getPoolId(), e);
                throw e;
            }
        }
    }
}
