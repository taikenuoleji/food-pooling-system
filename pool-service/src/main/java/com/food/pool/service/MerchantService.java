package com.food.pool.service;

import com.food.pool.dto.MenuItemVO;
import com.food.pool.dto.MerchantVO;

import java.util.List;

public interface MerchantService {

    List<MerchantVO> listMerchants(String category);

    MerchantVO getMerchantDetail(String merchantId);

    List<MenuItemVO> listMenuItems(String merchantId);
}
