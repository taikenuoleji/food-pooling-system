package com.food.order.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OrderListItemVO {
    private String orderId;
    private String merchantName;
    private String status;
    private Long myPayAmount;
    private LocalDateTime createdAt;
}
