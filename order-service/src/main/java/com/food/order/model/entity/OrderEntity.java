package com.food.order.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("orders")
public class OrderEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderId;
    private String poolId;
    private String merchantId;
    private String merchantName;
    private String status;
    private Long totalFoodAmount;
    private Long deliveryFee;
    private Long packagingFee;
    private Long couponDiscount;
    private Long totalAmount;
    private Integer memberCount;
    private String deliveryAddress;
    private LocalDateTime estimatedArrival;
    private LocalDateTime paidAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private String cancelReason;
    private String externalOrderId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
