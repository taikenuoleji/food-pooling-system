package com.food.payment.service;

import com.food.common.dto.SettlementDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 费用分摊计算器
 * 按设计文档 4.3 节实现
 */
@Component
public class FeeSplitCalculator {

    /**
     * 计算拼单费用分摊
     *
     * @param userIds          各用户ID（按加入顺序）
     * @param foodAmounts      各用户的菜品金额（分）
     * @param deliveryFee      配送费（分）
     * @param packagingFee     包装费（分）
     * @param couponDiscount   优惠券减免（分）
     * @return 各用户应付明细
     */
    public List<SettlementDTO> calculate(List<String> userIds,
                                         List<Long> foodAmounts,
                                         long deliveryFee,
                                         long packagingFee,
                                         long couponDiscount) {
        int memberCount = userIds.size();
        long totalFoodAmount = foodAmounts.stream().mapToLong(Long::longValue).sum();

        // 1. 按人头均摊配送费，余数由最后一人承担
        long[] deliveryShares = splitEvenly(deliveryFee, memberCount);

        // 2. 按人头均摊包装费，余数由最后一人承担
        long[] packagingShares = splitEvenly(packagingFee, memberCount);

        // 3. 按菜品金额比例分摊优惠券
        long[] couponShares = splitByRatio(couponDiscount, foodAmounts);

        // 4. 汇总每人应付
        List<SettlementDTO> result = new ArrayList<>();
        for (int i = 0; i < memberCount; i++) {
            long foodAmount = foodAmounts.get(i);
            long total = foodAmount - couponShares[i]
                    + deliveryShares[i]
                    + packagingShares[i];
            result.add(SettlementDTO.builder()
                    .userId(userIds.get(i))
                    .foodAmount(foodAmount)
                    .deliveryShare(deliveryShares[i])
                    .packagingShare(packagingShares[i])
                    .couponShare(couponShares[i])
                    .totalAmount(Math.max(total, 0))
                    .build());
        }
        return result;
    }

    /**
     * 按人头均分，余数由最后一人承担
     */
    private long[] splitEvenly(long totalAmount, int count) {
        if (count == 0) return new long[0];
        long[] shares = new long[count];
        long each = totalAmount / count;
        long remainder = totalAmount - each * count;
        Arrays.fill(shares, each);
        shares[count - 1] += remainder;
        return shares;
    }

    /**
     * 按菜品金额比例分摊，余数给金额最大者
     */
    private long[] splitByRatio(long totalAmount, List<Long> foodAmounts) {
        long totalFood = foodAmounts.stream().mapToLong(Long::longValue).sum();
        if (totalFood == 0) return new long[foodAmounts.size()];

        int count = foodAmounts.size();
        long[] shares = new long[count];
        long allocated = 0;

        for (int i = 0; i < count; i++) {
            BigDecimal ratio = BigDecimal.valueOf(foodAmounts.get(i))
                    .divide(BigDecimal.valueOf(totalFood), 10, RoundingMode.FLOOR);
            shares[i] = ratio.multiply(BigDecimal.valueOf(totalAmount))
                    .setScale(0, RoundingMode.FLOOR).longValue();
            allocated += shares[i];
        }
        // 余数给金额最大者
        long remainder = totalAmount - allocated;
        if (remainder > 0) {
            int maxIdx = 0;
            for (int i = 1; i < count; i++) {
                if (foodAmounts.get(i) > foodAmounts.get(maxIdx)) {
                    maxIdx = i;
                }
            }
            shares[maxIdx] += remainder;
        }
        return shares;
    }
}
