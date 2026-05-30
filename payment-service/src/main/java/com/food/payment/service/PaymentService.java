package com.food.payment.service;

import com.food.payment.dto.*;

import java.util.List;

public interface PaymentService {

    ChargeResultVO charge(String poolId, ChargeRequest request);

    RefundResultVO refund(String poolId, RefundRequest request);

    SettlementVO getSettlement(String poolId);

    List<PaymentRecordVO> getUserRecords(String userId, int page, int size);

    void payOrder(String orderId, String userId);
}
