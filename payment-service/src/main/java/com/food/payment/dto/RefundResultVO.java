package com.food.payment.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RefundResultVO {
    private String refundBatchId;
    private String poolId;
    private Long totalRefunded;
    private List<RefundItemResult> refunds;

    @Data
    @Builder
    public static class RefundItemResult {
        private String userId;
        private Long amount;
        private String status;
    }
}
