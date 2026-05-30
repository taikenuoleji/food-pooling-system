package com.food.pool.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PoolDetailVO {
    private String poolId;
    private String merchantId;
    private String merchantName;
    private String status;
    private FormationRule formationRule;
    private Integer currentMembers;
    private Long currentTotalAmount;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private List<ParticipantVO> participants;
}
