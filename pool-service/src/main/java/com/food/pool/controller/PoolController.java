package com.food.pool.controller;

import com.food.common.result.Result;
import com.food.pool.dto.CreatePoolRequest;
import com.food.pool.dto.JoinPoolRequest;
import com.food.pool.dto.PoolDetailVO;
import com.food.pool.dto.PoolListItemVO;
import com.food.pool.service.PoolService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/pools")
public class PoolController {

    private final PoolService poolService;

    @PostMapping
    public Result<PoolDetailVO> createPool(@RequestHeader("X-User-Id") String userId,
                                           @RequestBody CreatePoolRequest request) {
        return Result.success(poolService.createPool(userId, request));
    }

    @PostMapping("/{poolId}/join")
    public Result<PoolDetailVO> joinPool(@PathVariable String poolId,
                                         @RequestHeader("X-User-Id") String userId,
                                         @RequestBody JoinPoolRequest request) {
        return Result.success(poolService.joinPool(poolId, userId, request));
    }

    @PostMapping("/{poolId}/leave")
    public Result<Void> leavePool(@PathVariable String poolId,
                                  @RequestHeader("X-User-Id") String userId) {
        poolService.leavePool(poolId, userId);
        return Result.success();
    }

    @GetMapping("/{poolId}")
    public Result<PoolDetailVO> getPoolDetail(@PathVariable String poolId) {
        return Result.success(poolService.getPoolDetail(poolId));
    }

    @GetMapping("/my")
    public Result<List<PoolListItemVO>> listMyPools(@RequestHeader("X-User-Id") String userId,
                                                    @RequestParam(required = false) String status,
                                                    @RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "10") int size) {
        return Result.success(poolService.listMyPools(userId, status, page, size));
    }

    @GetMapping("/available")
    public Result<List<PoolListItemVO>> listAvailablePools(@RequestHeader("X-User-Id") String userId,
                                                           @RequestParam(defaultValue = "1") int page,
                                                           @RequestParam(defaultValue = "20") int size) {
        return Result.success(poolService.listAvailablePools(userId, page, size));
    }

    @GetMapping("/plaza")
    public Result<List<PoolListItemVO>> listPlazaPools(@RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "20") int size) {
        return Result.success(poolService.listPlazaPools(page, size));
    }

    @PostMapping("/{poolId}/items")
    public Result<PoolDetailVO> addItems(@PathVariable String poolId,
                                         @RequestHeader("X-User-Id") String userId,
                                         @RequestBody List<JoinPoolRequest.ItemRequest> items) {
        return Result.success(poolService.addItems(poolId, userId, items));
    }
}
