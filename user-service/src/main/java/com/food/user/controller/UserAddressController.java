package com.food.user.controller;

import com.food.common.result.Result;
import com.food.user.dto.AddressRequest;
import com.food.user.dto.AddressVO;
import com.food.user.service.UserAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserAddressController {

    private final UserAddressService addressService;

    @PostMapping("/addresses")
    public Result<AddressVO> addAddress(@RequestHeader("X-User-Id") String userId,
                                        @RequestBody AddressRequest request) {
        return Result.success(addressService.addAddress(userId, request));
    }

    @GetMapping("/{userId}/addresses")
    public Result<List<AddressVO>> listAddresses(@PathVariable String userId) {
        return Result.success(addressService.listAddresses(userId));
    }
}
