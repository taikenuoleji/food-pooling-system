package com.food.pool.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.food.common.exception.BusinessException;
import com.food.common.constants.BusinessConstants;
import com.food.pool.dto.MenuItemVO;
import com.food.pool.dto.MerchantVO;
import com.food.pool.mapper.MenuItemMapper;
import com.food.pool.mapper.MerchantMapper;
import com.food.pool.model.entity.MenuItemEntity;
import com.food.pool.model.entity.MerchantEntity;
import com.food.pool.service.MerchantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MerchantServiceImpl implements MerchantService {

    private final MerchantMapper merchantMapper;
    private final MenuItemMapper menuItemMapper;

    @Override
    public List<MerchantVO> listMerchants(String category) {
        LambdaQueryWrapper<MerchantEntity> wrapper = new LambdaQueryWrapper<MerchantEntity>()
                .eq(MerchantEntity::getStatus, "ACTIVE")
                .orderByDesc(MerchantEntity::getMonthlySales);

        if (category != null && !category.isEmpty() && !"all".equals(category)) {
            wrapper.eq(MerchantEntity::getCategory, category);
        }

        return merchantMapper.selectList(wrapper).stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public MerchantVO getMerchantDetail(String merchantId) {
        MerchantEntity merchant = merchantMapper.selectOne(
                new LambdaQueryWrapper<MerchantEntity>().eq(MerchantEntity::getMerchantId, merchantId));
        if (merchant == null) {
            throw new BusinessException(BusinessConstants.CODE_NOT_FOUND, "商家不存在");
        }
        return toVO(merchant);
    }

    @Override
    public List<MenuItemVO> listMenuItems(String merchantId) {
        return menuItemMapper.selectList(
                new LambdaQueryWrapper<MenuItemEntity>()
                        .eq(MenuItemEntity::getMerchantId, merchantId)
                        .eq(MenuItemEntity::getStatus, "AVAILABLE")
                        .orderByAsc(MenuItemEntity::getSortOrder))
                .stream().map(this::toItemVO).collect(Collectors.toList());
    }

    private MerchantVO toVO(MerchantEntity e) {
        return MerchantVO.builder()
                .merchantId(e.getMerchantId())
                .name(e.getName())
                .category(e.getCategory())
                .description(e.getDescription())
                .imageUrl(e.getImageUrl())
                .rating(e.getRating())
                .monthlySales(e.getMonthlySales())
                .deliveryTime(e.getDeliveryTime())
                .deliveryFee(e.getDeliveryFee())
                .minOrder(e.getMinOrder())
                .promo(e.getPromo())
                .build();
    }

    private MenuItemVO toItemVO(MenuItemEntity e) {
        return MenuItemVO.builder()
                .itemId(e.getItemId())
                .merchantId(e.getMerchantId())
                .name(e.getName())
                .description(e.getDescription())
                .price(e.getPrice())
                .imageUrl(e.getImageUrl())
                .category(e.getCategory())
                .build();
    }
}
