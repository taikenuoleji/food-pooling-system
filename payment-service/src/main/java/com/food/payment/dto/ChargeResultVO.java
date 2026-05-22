package com.food.payment.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChargeResultVO {
    private String paymentBatchId;
    private String poolId;
    private Long totalCharged;
    private List<ChargeItemResult> results;

    @Data
    @Builder
    public static class ChargeItemResult {
        private String userId;
        private Long amount;
        private String status;
    }
}
