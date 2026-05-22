package com.food.pool.feign;

import com.food.common.dto.UserInfoDTO;
import com.food.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", path = "/api/v1/users")
public interface UserFeignClient {

    @GetMapping("/{userId}/info")
    Result<UserInfoDTO> getUserInfo(@PathVariable("userId") String userId);
}
