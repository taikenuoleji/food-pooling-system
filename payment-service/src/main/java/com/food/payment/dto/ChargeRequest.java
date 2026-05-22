package com.food.payment.dto;

import lombok.Data;

import java.util.List;

@Data
public class ChargeRequest {
    private String poolId;
    private String orderId;
    private List<ChargeItem> charges;

    @Data
    public static class ChargeItem {
        private String userId;
        private Long foodAmount;
        private Long deliveryShare;
        private Long packagingShare;
        private Long totalAmount;
    }
}
