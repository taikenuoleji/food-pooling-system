package com.food.order.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("order_participant_settlements")
public class OrderParticipantSettlementEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderId;
    private String userId;
    private Long foodAmount;
    private Long deliveryShare;
    private Long packagingShare;
    private Long couponShare;
    private Long totalAmount;
    private String payStatus;
    private LocalDateTime createdAt;
}
