package com.food.pool.service;

import com.food.pool.model.entity.PoolEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 成团检测引擎
 */
@Slf4j
@Service
public class PoolFormationService {

    /**
     * 判断拼单是否满足成团条件（或逻辑，满足任一即成团）
     * minAmount=0 时表示不设金额门槛，只看人数
     */
    public boolean shouldForm(PoolEntity pool) {
        boolean memberMet = pool.getCurrentMembers() >= pool.getMinMembers();
        boolean amountMet = pool.getMinAmount() > 0
                && pool.getCurrentFoodAmount() >= pool.getMinAmount();
        boolean timeoutMet = LocalDateTime.now().isAfter(pool.getExpiresAt());

        if (memberMet || amountMet || timeoutMet) {
            log.info("拼单 {} 成团判定: members={}({}), amount={}({}), timeout={}({})",
                    pool.getPoolId(),
                    pool.getCurrentMembers(), memberMet,
                    pool.getCurrentFoodAmount(), amountMet,
                    pool.getExpiresAt(), timeoutMet);
            return true;
        }
        return false;
    }
}
