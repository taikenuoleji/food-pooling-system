package com.food.pool.controller;

import com.food.common.result.Result;
import com.food.pool.dto.MenuItemVO;
import com.food.pool.dto.MerchantVO;
import com.food.pool.service.MerchantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/merchants")
public class MerchantController {

    private final MerchantService merchantService;

    @GetMapping
    public Result<List<MerchantVO>> listMerchants(@RequestParam(required = false) String category) {
        return Result.success(merchantService.listMerchants(category));
    }

    @GetMapping("/{merchantId}")
    public Result<MerchantVO> getMerchantDetail(@PathVariable String merchantId) {
        return Result.success(merchantService.getMerchantDetail(merchantId));
    }

    @GetMapping("/{merchantId}/items")
    public Result<List<MenuItemVO>> listMenuItems(@PathVariable String merchantId) {
        return Result.success(merchantService.listMenuItems(merchantId));
    }
}
