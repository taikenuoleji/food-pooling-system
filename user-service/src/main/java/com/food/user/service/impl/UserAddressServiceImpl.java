package com.food.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.food.common.constants.BusinessConstants;
import com.food.common.utils.IdGenerator;
import com.food.user.dto.AddressRequest;
import com.food.user.dto.AddressVO;
import com.food.user.mapper.UserAddressMapper;
import com.food.user.model.entity.UserAddressEntity;
import com.food.user.service.UserAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserAddressServiceImpl implements UserAddressService {

    private final UserAddressMapper addressMapper;

    @Override
    @Transactional
    public AddressVO addAddress(String userId, AddressRequest request) {
        // 如果设为默认，先把该用户其他地址的默认标记取消
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            UserAddressEntity reset = new UserAddressEntity();
            reset.setIsDefault(0);
            addressMapper.update(reset, new LambdaQueryWrapper<UserAddressEntity>()
                    .eq(UserAddressEntity::getUserId, userId)
                    .eq(UserAddressEntity::getIsDefault, 1));
        }

        UserAddressEntity entity = new UserAddressEntity();
        entity.setAddressId(IdGenerator.generate(BusinessConstants.ID_PREFIX_ADDRESS));
        entity.setUserId(userId);
        entity.setLabel(request.getLabel());
        entity.setDetailAddress(request.getDetail());
        entity.setContactName(request.getContactName());
        entity.setContactPhone(request.getContactPhone());
        entity.setIsDefault(Boolean.TRUE.equals(request.getIsDefault()) ? 1 : 0);
        addressMapper.insert(entity);

        return AddressVO.builder()
                .addressId(entity.getAddressId())
                .label(entity.getLabel())
                .detail(entity.getDetailAddress())
                .contactName(entity.getContactName())
                .contactPhone(entity.getContactPhone())
                .isDefault(entity.getIsDefault() == 1)
                .build();
    }

    @Override
    public List<AddressVO> listAddresses(String userId) {
        List<UserAddressEntity> list = addressMapper.selectList(
                new LambdaQueryWrapper<UserAddressEntity>()
                        .eq(UserAddressEntity::getUserId, userId)
                        .orderByDesc(UserAddressEntity::getIsDefault)
                        .orderByDesc(UserAddressEntity::getCreatedAt));

        return list.stream().map(e -> AddressVO.builder()
                .addressId(e.getAddressId())
                .label(e.getLabel())
                .detail(e.getDetailAddress())
                .contactName(e.getContactName())
                .contactPhone(e.getContactPhone())
                .isDefault(e.getIsDefault() == 1)
                .build()).collect(Collectors.toList());
    }

    @Override
    public String getDefaultAddressId(String userId) {
        UserAddressEntity addr = addressMapper.selectOne(
                new LambdaQueryWrapper<UserAddressEntity>()
                        .eq(UserAddressEntity::getUserId, userId)
                        .eq(UserAddressEntity::getIsDefault, 1)
                        .last("LIMIT 1"));
        return addr != null ? addr.getAddressId() : null;
    }
}
