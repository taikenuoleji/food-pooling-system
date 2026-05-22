package com.food.common.event;

import lombok.Data;

import java.io.Serializable;

/**
 * 拼单解散事件
 * 生产者: pool-service
 * 消费者: payment-service (退款)
 */
@Data
public class PoolDissolvedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String poolId;
    private String reason;
}
