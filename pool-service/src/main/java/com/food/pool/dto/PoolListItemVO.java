package com.food.pool.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PoolListItemVO {
    private String poolId;
    private String merchantName;
    private String status;
    private String role;
    private Long mySubtotal;
    private LocalDateTime joinedAt;
}
