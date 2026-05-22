package com.food.pool.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pools")
public class PoolEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String poolId;
    private String inviteCode;
    private String creatorId;
    private String merchantId;
    private String merchantName;
    private String status;
    private Integer minMembers;
    private Long minAmount;
    private Integer deadlineMinutes;
    private Integer currentMembers;
    private Long currentFoodAmount;
    private String orderId;
    private Long deliveryFee;
    private Long packagingFee;
    private String remark;
    private LocalDateTime expiresAt;
    private LocalDateTime formedAt;
    private LocalDateTime dissolvedAt;
    private String dissolveReason;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
