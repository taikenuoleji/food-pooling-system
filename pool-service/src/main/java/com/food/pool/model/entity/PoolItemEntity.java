package com.food.pool.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pool_items")
public class PoolItemEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String poolId;
    private String participantId;
    private String userId;
    private String itemId;
    private String itemName;
    private Long unitPrice;
    private Integer quantity;
    private Long totalPrice;
    private LocalDateTime createdAt;
}
