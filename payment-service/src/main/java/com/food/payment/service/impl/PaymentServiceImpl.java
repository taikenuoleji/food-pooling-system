package com.food.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.food.common.constants.BusinessConstants;
import com.food.common.exception.BusinessException;
import com.food.common.utils.IdGenerator;
import com.food.payment.dto.*;
import com.food.payment.mapper.PaymentBatchMapper;
import com.food.payment.mapper.PaymentRecordMapper;
import com.food.payment.model.entity.PaymentBatchEntity;
import com.food.payment.model.entity.PaymentRecordEntity;
import com.food.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentBatchMapper batchMapper;
    private final PaymentRecordMapper recordMapper;

    @Override
    @Transactional
    public ChargeResultVO charge(String poolId, ChargeRequest request) {
        String batchId = IdGenerator.generate(BusinessConstants.ID_PREFIX_PAYMENT_BATCH);

        // 创建批次
        PaymentBatchEntity batch = new PaymentBatchEntity();
        batch.setBatchId(batchId);
        batch.setPoolId(poolId);
        batch.setOrderId(request.getOrderId());
        batch.setBatchType("CHARGE");
        batch.setStatus("PROCESSING");
        batch.setCreatedAt(LocalDateTime.now());
        batch.setUpdatedAt(LocalDateTime.now());

        long totalCharged = 0;
        List<ChargeResultVO.ChargeItemResult> results = new ArrayList<>();

        for (ChargeRequest.ChargeItem item : request.getCharges()) {
            String recordId = IdGenerator.generate(BusinessConstants.ID_PREFIX_PAYMENT_RECORD);

            PaymentRecordEntity record = new PaymentRecordEntity();
            record.setRecordId(recordId);
            record.setBatchId(batchId);
            record.setUserId(item.getUserId());
            record.setPoolId(poolId);
            record.setOrderId(request.getOrderId());
            record.setType("CHARGE");
            record.setAmount(item.getTotalAmount());
            record.setFoodAmount(item.getFoodAmount());
            record.setDeliveryShare(item.getDeliveryShare());
            record.setPackagingShare(item.getPackagingShare());
            record.setCouponShare(0L);
            record.setStatus("SUCCESS"); // 模拟成功
            record.setCreatedAt(LocalDateTime.now());
            record.setUpdatedAt(LocalDateTime.now());
            recordMapper.insert(record);

            totalCharged += item.getTotalAmount();

            results.add(ChargeResultVO.ChargeItemResult.builder()
                    .userId(item.getUserId())
                    .amount(item.getTotalAmount())
                    .status("CHARGED")
                    .build());

            log.info("扣款成功: 用户={}, 金额={}分", item.getUserId(), item.getTotalAmount());
        }

        batch.setTotalAmount(totalCharged);
        batch.setStatus("SUCCESS");
        batchMapper.insert(batch);

        return ChargeResultVO.builder()
                .paymentBatchId(batchId)
                .poolId(poolId)
                .totalCharged(totalCharged)
                .results(results)
                .build();
    }

    @Override
    @Transactional
    public RefundResultVO refund(String poolId, RefundRequest request) {
        String batchId = IdGenerator.generate(BusinessConstants.ID_PREFIX_PAYMENT_BATCH);

        // 查询该拼单下所有成功的扣款记录
        List<PaymentRecordEntity> chargeRecords = recordMapper.selectList(
                new LambdaQueryWrapper<PaymentRecordEntity>()
                        .eq(PaymentRecordEntity::getPoolId, poolId)
                        .eq(PaymentRecordEntity::getType, "CHARGE")
                        .eq(PaymentRecordEntity::getStatus, "SUCCESS"));

        PaymentBatchEntity batch = new PaymentBatchEntity();
        batch.setBatchId(batchId);
        batch.setPoolId(poolId);
        batch.setOrderId("");
        batch.setBatchType("REFUND");
        batch.setStatus("PROCESSING");
        batch.setCreatedAt(LocalDateTime.now());
        batch.setUpdatedAt(LocalDateTime.now());

        long totalRefunded = 0;
        List<RefundResultVO.RefundItemResult> refundItems = new ArrayList<>();

        for (PaymentRecordEntity charge : chargeRecords) {
            String recordId = IdGenerator.generate(BusinessConstants.ID_PREFIX_PAYMENT_RECORD);

            PaymentRecordEntity refundRecord = new PaymentRecordEntity();
            refundRecord.setRecordId(recordId);
            refundRecord.setBatchId(batchId);
            refundRecord.setUserId(charge.getUserId());
            refundRecord.setPoolId(poolId);
            refundRecord.setType("REFUND");
            refundRecord.setAmount(charge.getAmount());
            refundRecord.setFoodAmount(charge.getFoodAmount());
            refundRecord.setDeliveryShare(charge.getDeliveryShare());
            refundRecord.setPackagingShare(charge.getPackagingShare());
            refundRecord.setCouponShare(charge.getCouponShare());
            refundRecord.setStatus("SUCCESS");
            refundRecord.setCreatedAt(LocalDateTime.now());
            refundRecord.setUpdatedAt(LocalDateTime.now());
            recordMapper.insert(refundRecord);

            totalRefunded += charge.getAmount();

            refundItems.add(RefundResultVO.RefundItemResult.builder()
                    .userId(charge.getUserId())
                    .amount(charge.getAmount())
                    .status("REFUNDED")
                    .build());
        }

        batch.setTotalAmount(totalRefunded);
        batch.setStatus("SUCCESS");
        batchMapper.insert(batch);

        log.info("拼单 {} 退款完成，总退款={}分", poolId, totalRefunded);

        return RefundResultVO.builder()
                .refundBatchId(batchId)
                .poolId(poolId)
                .totalRefunded(totalRefunded)
                .refunds(refundItems)
                .build();
    }

    @Override
    public SettlementVO getSettlement(String poolId) {
        List<PaymentRecordEntity> records = recordMapper.selectList(
                new LambdaQueryWrapper<PaymentRecordEntity>()
                        .eq(PaymentRecordEntity::getPoolId, poolId)
                        .eq(PaymentRecordEntity::getType, "CHARGE"));

        if (records.isEmpty()) {
            throw new BusinessException(BusinessConstants.CODE_NOT_FOUND, "无支付记录");
        }

        // 汇总公共费用
        long deliveryFee = records.stream().mapToLong(PaymentRecordEntity::getDeliveryShare).sum();
        long packagingFee = records.stream().mapToLong(PaymentRecordEntity::getPackagingShare).sum();

        List<SettlementVO.SettlementItemVO> items = records.stream().map(r ->
                SettlementVO.SettlementItemVO.builder()
                        .userId(r.getUserId())
                        .nickname("")
                        .foodItems(new ArrayList<>())
                        .foodAmount(r.getFoodAmount())
                        .deliveryShare(r.getDeliveryShare())
                        .packagingShare(r.getPackagingShare())
                        .totalAmount(r.getAmount())
                        .payStatus(r.getStatus())
                        .build()
        ).collect(Collectors.toList());

        return SettlementVO.builder()
                .poolId(poolId)
                .orderId(records.get(0).getOrderId())
                .splitMethod("ITEM_BASED")
                .commonFees(SettlementVO.CommonFeeVO.builder()
                        .deliveryFee(deliveryFee)
                        .packagingFee(packagingFee)
                        .build())
                .settlements(items)
                .build();
    }

    @Override
    public List<PaymentRecordVO> getUserRecords(String userId, int page, int size) {
        LambdaQueryWrapper<PaymentRecordEntity> wrapper = new LambdaQueryWrapper<PaymentRecordEntity>()
                .eq(PaymentRecordEntity::getUserId, userId)
                .orderByDesc(PaymentRecordEntity::getCreatedAt)
                .last("LIMIT " + (page - 1) * size + "," + size);

        List<PaymentRecordEntity> records = recordMapper.selectList(wrapper);

        return records.stream().map(r -> PaymentRecordVO.builder()
                .recordId(r.getRecordId())
                .poolId(r.getPoolId())
                .type(r.getType())
                .amount(r.getAmount())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .build()).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void payOrder(String orderId, String userId) {
        log.info("用户 {} 支付订单 {}", userId, orderId);

        // 查询该用户该订单的扣款记录
        List<PaymentRecordEntity> records = recordMapper.selectList(
                new LambdaQueryWrapper<PaymentRecordEntity>()
                        .eq(PaymentRecordEntity::getOrderId, orderId)
                        .eq(PaymentRecordEntity::getUserId, userId)
                        .eq(PaymentRecordEntity::getType, "CHARGE"));

        if (records.isEmpty()) {
            // 如果没有扣款记录，创建一条新的支付记录
            String recordId = IdGenerator.generate(BusinessConstants.ID_PREFIX_PAYMENT_RECORD);
            PaymentRecordEntity record = new PaymentRecordEntity();
            record.setRecordId(recordId);
            record.setBatchId("MANUAL");
            record.setUserId(userId);
            record.setOrderId(orderId);
            record.setType("CHARGE");
            record.setAmount(0L); // 金额从订单获取，这里简化处理
            record.setStatus("SUCCESS");
            record.setCreatedAt(LocalDateTime.now());
            record.setUpdatedAt(LocalDateTime.now());
            recordMapper.insert(record);
            log.info("创建支付记录: {}", recordId);
        } else {
            // 更新现有记录状态
            for (PaymentRecordEntity record : records) {
                if ("PENDING".equals(record.getStatus())) {
                    record.setStatus("SUCCESS");
                    record.setUpdatedAt(LocalDateTime.now());
                    recordMapper.updateById(record);
                    log.info("更新支付记录: {} -> SUCCESS", record.getRecordId());
                }
            }
        }

        log.info("订单 {} 支付完成", orderId);
    }
}
