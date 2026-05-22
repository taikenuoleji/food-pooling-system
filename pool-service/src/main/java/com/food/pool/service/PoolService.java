package com.food.pool.service;

import com.food.pool.dto.CreatePoolRequest;
import com.food.pool.dto.JoinPoolRequest;
import com.food.pool.dto.PoolDetailVO;
import com.food.pool.dto.PoolListItemVO;

import java.util.List;

public interface PoolService {

    PoolDetailVO createPool(String creatorId, CreatePoolRequest request);

    PoolDetailVO joinPool(String poolId, String userId, JoinPoolRequest request);

    void leavePool(String poolId, String userId);

    PoolDetailVO getPoolDetail(String poolId);

    List<PoolListItemVO> listMyPools(String userId, String status, int page, int size);

    List<PoolListItemVO> listAvailablePools(String userId, int page, int size);

    PoolDetailVO addItems(String poolId, String userId, List<JoinPoolRequest.ItemRequest> items);
}
