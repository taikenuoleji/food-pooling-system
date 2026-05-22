package com.food.pool.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("merchants")
public class MerchantEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
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
    private String status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
