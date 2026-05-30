package com.food.pool.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ParticipantVO {
    private String userId;
    private String nickname;
    private String role;
    private List<ItemVO> items;
    private Long subtotal;
    private LocalDateTime joinedAt;

    @Data
    @Builder
    public static class ItemVO {
        private String itemName;
        private Long unitPrice;
        private Integer quantity;
    }
}
