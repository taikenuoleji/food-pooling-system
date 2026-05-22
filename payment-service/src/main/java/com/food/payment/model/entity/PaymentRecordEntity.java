package com.food.payment.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("payment_records")
public class PaymentRecordEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String recordId;
    private String batchId;
    private String userId;
    private String poolId;
    private String orderId;
    private String type;
    private Long amount;
    private Long foodAmount;
    private Long deliveryShare;
    private Long packagingShare;
    private Long couponShare;
    private String payChannel;
    private String channelTradeNo;
    private String status;
    private String failReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
