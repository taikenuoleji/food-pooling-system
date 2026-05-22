package com.food.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 费用分摊结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String userId;
    private long foodAmount;
    private long deliveryShare;
    private long packagingShare;
    private long couponShare;
    private long totalAmount;
}
