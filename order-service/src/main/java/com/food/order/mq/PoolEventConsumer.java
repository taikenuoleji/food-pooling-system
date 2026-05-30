package com.food.order.mq;

import com.food.common.event.PoolFormedEvent;
import com.food.order.dto.CreateOrderRequest;
import com.food.order.feign.PaymentFeignClient;
import com.food.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 监听拼单成团事件 → 生成订单 → 调用扣款
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = "pool-formed-topic", consumerGroup = "order-consumer-group")
public class PoolEventConsumer implements RocketMQListener<PoolFormedEvent> {

    private final OrderService orderService;
    private final PaymentFeignClient paymentFeignClient;

    @Override
    public void onMessage(PoolFormedEvent event) {
        log.info("收到成团事件: poolId={}", event.getPoolId());

        try {
            // 1. 生成订单
            CreateOrderRequest request = buildCreateOrderRequest(event);
            String orderId = orderService.createOrder(request);
            log.info("订单 {} 创建成功", orderId);

            // 2. 调用支付服务扣款
            Map<String, Object> chargeReq = buildChargeRequest(event, orderId);
            paymentFeignClient.charge(event.getPoolId(), chargeReq);
            log.info("扣款请求已发送: poolId={}, orderId={}", event.getPoolId(), orderId);

        } catch (Exception e) {
            log.error("处理成团事件失败: poolId={}", event.getPoolId(), e);
            throw e; // 抛出让 MQ 重试
        }
    }

    private CreateOrderRequest buildCreateOrderRequest(PoolFormedEvent event) {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setPoolId(event.getPoolId());
        request.setMerchantId(event.getMerchantId());
        request.setMerchantName(event.getMerchantName());
        request.setDeliveryFee(event.getDeliveryFee());
        request.setPackagingFee(event.getPackagingFee());
        request.setMemberCount(event.getMemberCount());

        // 汇总菜品（从参与者数据中聚合，简化处理）
        request.setItems(new ArrayList<>());
        request.setSettlements(new ArrayList<>());

        for (PoolFormedEvent.Participant p : event.getParticipants()) {
            CreateOrderRequest.SettlementDTO s = new CreateOrderRequest.SettlementDTO();
            s.setUserId(p.getUserId());
            s.setFoodAmount(p.getFoodAmount());
            s.setDeliveryShare(p.getDeliveryShare());
            s.setPackagingShare(p.getPackagingShare());
            s.setCouponShare(p.getCouponShare());
            s.setTotalAmount(p.getTotalAmount());
            request.getSettlements().add(s);
        }

        return request;
    }

    private Map<String, Object> buildChargeRequest(PoolFormedEvent event, String orderId) {
        Map<String, Object> chargeReq = new HashMap<>();
        chargeReq.put("poolId", event.getPoolId());
        chargeReq.put("orderId", orderId);

        List<Map<String, Object>> charges = new ArrayList<>();
        for (PoolFormedEvent.Participant p : event.getParticipants()) {
            Map<String, Object> charge = new HashMap<>();
            charge.put("userId", p.getUserId());
            charge.put("foodAmount", p.getFoodAmount());
            charge.put("deliveryShare", p.getDeliveryShare());
            charge.put("packagingShare", p.getPackagingShare());
            charge.put("totalAmount", p.getTotalAmount());
            charges.add(charge);
        }
        chargeReq.put("charges", charges);
        return chargeReq;
    }
}
