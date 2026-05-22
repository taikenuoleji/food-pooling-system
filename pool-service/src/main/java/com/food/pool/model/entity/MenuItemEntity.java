package com.food.pool.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("menu_items")
public class MenuItemEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String itemId;
    private String merchantId;
    private String name;
    private String description;
    private Long price;
    private String imageUrl;
    private String category;
    private String status;
    private Integer sortOrder;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
