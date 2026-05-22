package com.food.payment.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PaymentRecordVO {
    private String recordId;
    private String poolId;
    private String type;
    private Long amount;
    private String status;
    private LocalDateTime createdAt;
}
