package com.food.pool.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class MerchantVO {
    private String merchantId;
    private String name;
    private String category;
    private String description;
    private String imageUrl;
    private BigDecimal rating;
    private Integer monthlySales;
    private String deliveryTime;
    private Long deliveryFee;
    private Long minOrder;
    private String promo;
}
