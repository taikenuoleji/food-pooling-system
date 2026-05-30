package com.food.payment.controller;

import com.food.common.result.Result;
import com.food.payment.dto.*;
import com.food.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 拼单成团扣款（内部接口）
     */
    @PostMapping("/pool/{poolId}/charge")
    public Result<ChargeResultVO> charge(@PathVariable String poolId,
                                         @RequestBody ChargeRequest request) {
        return Result.success(paymentService.charge(poolId, request));
    }

    /**
     * 拼单退款（内部接口）
     */
    @PostMapping("/pool/{poolId}/refund")
    public Result<RefundResultVO> refund(@PathVariable String poolId,
                                         @RequestBody RefundRequest request) {
        return Result.success(paymentService.refund(poolId, request));
    }

    /**
     * 查询拼单费用结算明细
     */
    @GetMapping("/pool/{poolId}/settlement")
    public Result<SettlementVO> getSettlement(@PathVariable String poolId) {
        return Result.success(paymentService.getSettlement(poolId));
    }

    /**
     * 查询用户支付记录
     */
    @GetMapping("/user/{userId}/records")
    public Result<List<PaymentRecordVO>> getUserRecords(@PathVariable String userId,
                                                        @RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "10") int size) {
        return Result.success(paymentService.getUserRecords(userId, page, size));
    }

    /**
     * 按订单支付（用户确认支付）
     */
    @PostMapping("/order/{orderId}/pay")
    public Result<String> payOrder(@PathVariable String orderId,
                                   @RequestHeader("X-User-Id") String userId) {
        paymentService.payOrder(orderId, userId);
        return Result.success("支付成功");
    }
}
