package com.food.common.event;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 拼单成团事件
 * 生产者: pool-service
 * 消费者: order-service (生成订单), payment-service (扣款)
 */
@Data
public class PoolFormedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String poolId;
    private String merchantId;
    private String merchantName;
    private String creatorAddressId;
    private Integer memberCount;
    private Long deliveryFee;
    private Long packagingFee;
    private List<Participant> participants;

    @Data
    public static class Participant implements Serializable {
        private static final long serialVersionUID = 1L;

        private String userId;
        private Long foodAmount;
        private Long deliveryShare;
        private Long packagingShare;
        private Long couponShare;
        private Long totalAmount;
    }
}
