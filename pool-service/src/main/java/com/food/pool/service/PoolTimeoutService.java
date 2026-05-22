package com.food.pool.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.food.common.constants.BusinessConstants;
import com.food.common.event.PoolDissolvedEvent;
import com.food.pool.mapper.PoolMapper;
import com.food.pool.model.entity.PoolEntity;
import com.food.pool.mq.PoolEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 拼单超时定时扫描任务
 * 每30秒扫描一次，处理超时未解散的拼单
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PoolTimeoutService {

    private final PoolMapper poolMapper;
    private final PoolEventProducer eventProducer;

    @Scheduled(fixedRate = 30000)
    public void scanExpiredPools() {
        List<PoolEntity> expiredPools = poolMapper.selectList(
                new LambdaQueryWrapper<PoolEntity>()
                        .in(PoolEntity::getStatus, BusinessConstants.POOL_STATUS_CREATED, BusinessConstants.POOL_STATUS_FORMING)
                        .lt(PoolEntity::getExpiresAt, LocalDateTime.now()));

        for (PoolEntity pool : expiredPools) {
            try {
                int affected = poolMapper.update(null, new LambdaUpdateWrapper<PoolEntity>()
                        .eq(PoolEntity::getPoolId, pool.getPoolId())
                        .eq(PoolEntity::getVersion, pool.getVersion())
                        .in(PoolEntity::getStatus, BusinessConstants.POOL_STATUS_CREATED, BusinessConstants.POOL_STATUS_FORMING)
                        .set(PoolEntity::getStatus, BusinessConstants.POOL_STATUS_DISSOLVED)
                        .set(PoolEntity::getDissolvedAt, LocalDateTime.now())
                        .set(PoolEntity::getDissolveReason, "POOL_DISSOLVED_TIMEOUT")
                        .setSql("version = version + 1"));

                if (affected > 0) {
                    log.info("超时解散拼单: {}", pool.getPoolId());
                    PoolDissolvedEvent event = new PoolDissolvedEvent();
                    event.setPoolId(pool.getPoolId());
                    event.setReason("POOL_DISSOLVED_TIMEOUT");
                    eventProducer.sendPoolDissolved(event);
                }
            } catch (Exception e) {
                log.error("解散拼单 {} 失败", pool.getPoolId(), e);
            }
        }
    }
}
