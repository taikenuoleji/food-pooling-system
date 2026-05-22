package com.food.payment.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("payment_batches")
public class PaymentBatchEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String batchId;
    private String poolId;
    private String orderId;
    private String batchType;
    private Long totalAmount;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
