package com.food.order.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("order_items")
public class OrderItemEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderId;
    private String itemId;
    private String itemName;
    private Long unitPrice;
    private Integer totalQuantity;
    private Long totalPrice;
    private LocalDateTime createdAt;
}
