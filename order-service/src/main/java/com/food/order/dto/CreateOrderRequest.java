package com.food.order.dto;

import lombok.Data;

import java.util.List;

/**
 * 成团后创建订单的内部请求
 */
@Data
public class CreateOrderRequest {
    private String poolId;
    private String merchantId;
    private String merchantName;
    private String creatorAddressId;
    private Long deliveryFee;
    private Long packagingFee;
    private Integer memberCount;
    private List<OrderItemDTO> items;
    private List<SettlementDTO> settlements;

    @Data
    public static class OrderItemDTO {
        private String itemId;
        private String itemName;
        private Long unitPrice;
        private Integer totalQuantity;
        private Long totalPrice;
    }

    @Data
    public static class SettlementDTO {
        private String userId;
        private Long foodAmount;
        private Long deliveryShare;
        private Long packagingShare;
        private Long totalAmount;
    }
}
