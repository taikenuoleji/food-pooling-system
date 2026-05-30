package com.food.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.food.common.constants.BusinessConstants;
import com.food.common.event.OrderCancelledEvent;
import com.food.common.exception.BusinessException;
import com.food.common.utils.IdGenerator;
import com.food.order.dto.CreateOrderRequest;
import com.food.order.dto.OrderDetailVO;
import com.food.order.dto.OrderListItemVO;
import com.food.order.mapper.OrderItemMapper;
import com.food.order.mapper.OrderMapper;
import com.food.order.mapper.OrderParticipantSettlementMapper;
import com.food.order.model.entity.OrderEntity;
import com.food.order.model.entity.OrderItemEntity;
import com.food.order.model.entity.OrderParticipantSettlementEntity;
import com.food.order.mq.OrderEventProducer;
import com.food.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderParticipantSettlementMapper settlementMapper;
    private final OrderEventProducer eventProducer;

    @Override
    @Transactional
    public String createOrder(CreateOrderRequest request) {
        // 检查是否已存在该拼单的订单
        OrderEntity existingOrder = orderMapper.selectOne(
                new LambdaQueryWrapper<OrderEntity>().eq(OrderEntity::getPoolId, request.getPoolId()));
        if (existingOrder != null) {
            log.info("拼单 {} 的订单已存在: {}", request.getPoolId(), existingOrder.getOrderId());
            return existingOrder.getOrderId();
        }

        String orderId = IdGenerator.generate(BusinessConstants.ID_PREFIX_ORDER);

        // 汇总菜品金额（从items或settlements中计算）
        long totalFoodAmount = 0;
        long totalCouponDiscount = 0;
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (CreateOrderRequest.OrderItemDTO item : request.getItems()) {
                totalFoodAmount += item.getTotalPrice();
            }
        }
        if (request.getSettlements() != null) {
            // 从settlements中汇总
            for (CreateOrderRequest.SettlementDTO s : request.getSettlements()) {
                totalFoodAmount += s.getFoodAmount() != null ? s.getFoodAmount() : 0L;
                totalCouponDiscount += s.getCouponShare() != null ? s.getCouponShare() : 0L;
            }
        }

        long totalAmount = totalFoodAmount + request.getDeliveryFee() + request.getPackagingFee();

        OrderEntity order = new OrderEntity();
        order.setOrderId(orderId);
        order.setPoolId(request.getPoolId());
        order.setMerchantId(request.getMerchantId());
        order.setMerchantName(request.getMerchantName());
        order.setStatus(BusinessConstants.ORDER_STATUS_PENDING);
        order.setTotalFoodAmount(totalFoodAmount);
        order.setDeliveryFee(request.getDeliveryFee());
        order.setPackagingFee(request.getPackagingFee());
        order.setCouponDiscount(totalCouponDiscount);
        order.setTotalAmount(totalAmount);
        order.setMemberCount(request.getMemberCount());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.insert(order);

        // 插入订单菜品
        for (CreateOrderRequest.OrderItemDTO item : request.getItems()) {
            OrderItemEntity orderItem = new OrderItemEntity();
            orderItem.setOrderId(orderId);
            orderItem.setItemId(item.getItemId());
            orderItem.setItemName(item.getItemName());
            orderItem.setUnitPrice(item.getUnitPrice());
            orderItem.setTotalQuantity(item.getTotalQuantity());
            orderItem.setTotalPrice(item.getTotalPrice());
            orderItem.setCreatedAt(LocalDateTime.now());
            orderItemMapper.insert(orderItem);
        }

        // 插入分摊明细
        for (CreateOrderRequest.SettlementDTO s : request.getSettlements()) {
            OrderParticipantSettlementEntity settlement = new OrderParticipantSettlementEntity();
            settlement.setSettlementId(IdGenerator.generate("STL"));
            settlement.setOrderId(orderId);
            settlement.setUserId(s.getUserId());
            settlement.setFoodAmount(s.getFoodAmount());
            settlement.setDeliveryShare(s.getDeliveryShare());
            settlement.setPackagingShare(s.getPackagingShare());
            settlement.setCouponShare(s.getCouponShare() != null ? s.getCouponShare() : 0L);
            settlement.setTotalAmount(s.getTotalAmount());
            settlement.setPayStatus("PENDING");
            settlement.setCreatedAt(LocalDateTime.now());
            settlementMapper.insert(settlement);
        }

        log.info("订单 {} 创建成功，关联拼单 {}", orderId, request.getPoolId());
        return orderId;
    }

    @Override
    public OrderDetailVO getOrderDetail(String orderId) {
        OrderEntity order = orderMapper.selectOne(
                new LambdaQueryWrapper<OrderEntity>().eq(OrderEntity::getOrderId, orderId));
        if (order == null) {
            throw new BusinessException(BusinessConstants.CODE_NOT_FOUND, "订单不存在");
        }

        List<OrderItemEntity> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItemEntity>().eq(OrderItemEntity::getOrderId, orderId));

        List<OrderParticipantSettlementEntity> settlements = settlementMapper.selectList(
                new LambdaQueryWrapper<OrderParticipantSettlementEntity>()
                        .eq(OrderParticipantSettlementEntity::getOrderId, orderId));

        List<OrderDetailVO.ItemVO> itemVOs = items.stream().map(i ->
                OrderDetailVO.ItemVO.builder()
                        .itemName(i.getItemName())
                        .quantity(i.getTotalQuantity())
                        .totalPrice(i.getTotalPrice())
                        .build()
        ).collect(Collectors.toList());

        List<OrderDetailVO.ParticipantSettlementVO> settlementVOs = settlements.stream().map(s ->
                OrderDetailVO.ParticipantSettlementVO.builder()
                        .userId(s.getUserId())
                        .nickname("")
                        .foodAmount(s.getFoodAmount())
                        .deliveryShare(s.getDeliveryShare())
                        .packagingShare(s.getPackagingShare())
                        .couponShare(s.getCouponShare())
                        .totalPay(s.getTotalAmount())
                        .build()
        ).collect(Collectors.toList());

        return OrderDetailVO.builder()
                .orderId(order.getOrderId())
                .poolId(order.getPoolId())
                .merchantName(order.getMerchantName())
                .status(order.getStatus())
                .totalFoodAmount(order.getTotalFoodAmount())
                .deliveryFee(order.getDeliveryFee())
                .packagingFee(order.getPackagingFee())
                .couponDiscount(order.getCouponDiscount())
                .totalAmount(order.getTotalAmount())
                .memberCount(order.getMemberCount())
                .items(itemVOs)
                .participants(settlementVOs)
                .estimatedArrival(order.getEstimatedArrival())
                .createdAt(order.getCreatedAt())
                .build();
    }

    @Override
    public List<OrderListItemVO> listMyOrders(String userId, String status, int page, int size) {
        // 通过分摊明细表查出用户相关的订单ID
        List<OrderParticipantSettlementEntity> settlements = settlementMapper.selectList(
                new LambdaQueryWrapper<OrderParticipantSettlementEntity>()
                        .eq(OrderParticipantSettlementEntity::getUserId, userId));

        if (settlements.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> orderIds = settlements.stream()
                .map(OrderParticipantSettlementEntity::getOrderId)
                .distinct()
                .collect(Collectors.toList());

        LambdaQueryWrapper<OrderEntity> wrapper = new LambdaQueryWrapper<OrderEntity>()
                .in(OrderEntity::getOrderId, orderIds)
                .orderByDesc(OrderEntity::getCreatedAt);

        if (status != null && !status.isEmpty()) {
            wrapper.eq(OrderEntity::getStatus, status);
        }

        wrapper.last("LIMIT " + (page - 1) * size + "," + size);
        List<OrderEntity> orders = orderMapper.selectList(wrapper);

        // 建立 userId -> totalAmount 映射
        var settlementMap = settlements.stream()
                .collect(Collectors.toMap(
                        s -> s.getOrderId() + "_" + s.getUserId(),
                        OrderParticipantSettlementEntity::getTotalAmount));

        return orders.stream().map(o -> OrderListItemVO.builder()
                .orderId(o.getOrderId())
                .merchantName(o.getMerchantName())
                .status(o.getStatus())
                .myPayAmount(settlementMap.getOrDefault(o.getOrderId() + "_" + userId, 0L))
                .createdAt(o.getCreatedAt())
                .build()).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void cancelOrder(String orderId, String userId) {
        OrderEntity order = orderMapper.selectOne(
                new LambdaQueryWrapper<OrderEntity>().eq(OrderEntity::getOrderId, orderId));
        if (order == null) {
            throw new BusinessException(BusinessConstants.CODE_NOT_FOUND, "订单不存在");
        }
        if (!BusinessConstants.ORDER_STATUS_PENDING.equals(order.getStatus())) {
            throw new BusinessException(BusinessConstants.CODE_POOL_CONFLICT, "订单已确认，无法取消");
        }

        order.setStatus(BusinessConstants.ORDER_STATUS_CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancelReason("用户取消");
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);

        log.info("订单 {} 已取消", orderId);

        OrderCancelledEvent event = new OrderCancelledEvent();
        event.setOrderId(orderId);
        event.setPoolId(order.getPoolId());
        eventProducer.sendOrderCancelled(event);
    }
}
