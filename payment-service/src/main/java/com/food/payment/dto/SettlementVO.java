package com.food.payment.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SettlementVO {
    private String poolId;
    private String orderId;
    private String splitMethod;
    private CommonFeeVO commonFees;
    private List<SettlementItemVO> settlements;

    @Data
    @Builder
    public static class CommonFeeVO {
        private Long deliveryFee;
        private Long packagingFee;
    }

    @Data
    @Builder
    public static class SettlementItemVO {
        private String userId;
        private String nickname;
        private List<FoodItemVO> foodItems;
        private Long foodAmount;
        private Long deliveryShare;
        private Long packagingShare;
        private Long totalAmount;
        private String payStatus;
    }

    @Data
    @Builder
    public static class FoodItemVO {
        private String itemName;
        private Long price;
    }
}
