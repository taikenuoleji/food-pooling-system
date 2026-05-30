package com.food.pool.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PoolListItemVO {
    private String poolId;
    private String merchantName;
    private String merchantId;
    private String status;
    private String role;
    private Long mySubtotal;
    private LocalDateTime joinedAt;
    // 广场展示用
    private String creatorId;
    private String creatorName;
    private Integer currentMembers;
    private Integer minMembers;
    private Long currentFoodAmount;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private Long deliveryFee;
    private Long minOrder;
}
