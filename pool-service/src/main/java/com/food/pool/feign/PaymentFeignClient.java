package com.food.pool.feign;

import com.food.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "payment-service", path = "/api/v1/payments")
public interface PaymentFeignClient {

    @PostMapping("/pool/{poolId}/charge")
    Result<Map<String, Object>> charge(@PathVariable("poolId") String poolId,
                                       @RequestBody Map<String, Object> chargeRequest);

    @PostMapping("/pool/{poolId}/refund")
    Result<Map<String, Object>> refund(@PathVariable("poolId") String poolId,
                                       @RequestBody Map<String, Object> refundRequest);
}
