package com.food.pool.mq;

import com.food.common.event.PoolDissolvedEvent;
import com.food.common.event.PoolFormedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * 拼单事件 MQ 生产者
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PoolEventProducer {

    private final RocketMQTemplate rocketMQTemplate;

    private static final String POOL_FORMED_TOPIC = "pool-formed-topic";
    private static final String POOL_DISSOLVED_TOPIC = "pool-dissolved-topic";

    public void sendPoolFormed(PoolFormedEvent event) {
        log.info("发送成团事件: poolId={}", event.getPoolId());
        rocketMQTemplate.send(POOL_FORMED_TOPIC,
                MessageBuilder.withPayload(event).build());
    }

    public void sendPoolDissolved(PoolDissolvedEvent event) {
        log.info("发送解散事件: poolId={}, reason={}", event.getPoolId(), event.getReason());
        rocketMQTemplate.send(POOL_DISSOLVED_TOPIC,
                MessageBuilder.withPayload(event).build());
    }
}
