package com.food.payment.dto;

import lombok.Data;

@Data
public class RefundRequest {
    private String poolId;
    private String reason;
}
