package com.food.pool.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MenuItemVO {
    private String itemId;
    private String merchantId;
    private String name;
    private String description;
    private Long price;
    private String imageUrl;
    private String category;
}
