package com.food.pool.dto;

import lombok.Data;

import java.util.List;

@Data
public class JoinPoolRequest {
    private String addressId;
    private List<ItemRequest> items;
    private String remark;

    @Data
    public static class ItemRequest {
        private String itemId;
        private String itemName;
        private Long unitPrice;
        private Integer quantity;
    }
}
