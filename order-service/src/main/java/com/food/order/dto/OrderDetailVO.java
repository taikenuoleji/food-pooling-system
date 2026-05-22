package com.food.order.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderDetailVO {
    private String orderId;
    private String poolId;
    private String merchantName;
    private String status;
    private Long totalAmount;
    private Long deliveryFee;
    private List<ItemVO> items;
    private List<ParticipantSettlementVO> participants;
    private LocalDateTime estimatedArrival;
    private LocalDateTime createdAt;

    @Data
    @Builder
    public static class ItemVO {
        private String itemName;
        private Integer quantity;
        private Long totalPrice;
    }

    @Data
    @Builder
    public static class ParticipantSettlementVO {
        private String userId;
        private String nickname;
        private Long foodAmount;
        private Long deliveryShare;
        private Long packagingShare;
        private Long totalPay;
    }
}
