package com.food.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.food.common.constants.BusinessConstants;
import com.food.common.dto.UserInfoDTO;
import com.food.common.exception.BusinessException;
import com.food.common.utils.IdGenerator;
import com.food.common.utils.JwtUtil;
import com.food.user.dto.LoginRequest;
import com.food.user.dto.RegisterRequest;
import com.food.user.dto.UserVO;
import com.food.user.mapper.UserMapper;
import com.food.user.model.entity.UserEntity;
import com.food.user.service.UserAddressService;
import com.food.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserAddressService userAddressService;

    @Override
    @Transactional
    public UserVO register(RegisterRequest request) {
        // 检查手机号是否已注册
        UserEntity existing = userMapper.selectOne(
                new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getPhone, request.getPhone()));
        if (existing != null) {
            throw new BusinessException(BusinessConstants.CODE_PARAM_ERROR, "手机号已注册");
        }

        UserEntity user = new UserEntity();
        user.setUserId(IdGenerator.generate(BusinessConstants.ID_PREFIX_USER));
        user.setPhone(request.getPhone());
        user.setNickname(request.getNickname());
        user.setStatus(1);
        userMapper.insert(user);

        return buildUserVO(user);
    }

    @Override
    public UserVO login(LoginRequest request) {
        UserEntity user = userMapper.selectOne(
                new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getPhone, request.getPhone()));
        if (user == null) {
            throw new BusinessException(BusinessConstants.CODE_NOT_FOUND, "用户不存在");
        }
        if (user.getStatus() != 1) {
            throw new BusinessException(BusinessConstants.CODE_UNAUTHORIZED, "用户已被禁用");
        }

        return buildUserVO(user);
    }

    @Override
    public UserVO getUserById(String userId) {
        UserEntity user = userMapper.selectOne(
                new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getUserId, userId));
        if (user == null) {
            throw new BusinessException(BusinessConstants.CODE_NOT_FOUND, "用户不存在");
        }
        return buildUserVO(user);
    }

    @Override
    public UserInfoDTO getUserInfo(String userId) {
        UserEntity user = userMapper.selectOne(
                new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getUserId, userId));
        if (user == null) {
            throw new BusinessException(BusinessConstants.CODE_NOT_FOUND, "用户不存在");
        }
        UserInfoDTO dto = new UserInfoDTO();
        dto.setUserId(user.getUserId());
        dto.setNickname(user.getNickname());
        dto.setPhone(user.getPhone());
        return dto;
    }

    private UserVO buildUserVO(UserEntity user) {
        String token = JwtUtil.generateToken(user.getUserId());
        String defaultAddrId = userAddressService.getDefaultAddressId(user.getUserId());

        return UserVO.builder()
                .userId(user.getUserId())
                .phone(maskPhone(user.getPhone()))
                .nickname(user.getNickname())
                .avatar(user.getAvatarUrl())
                .defaultAddressId(defaultAddrId)
                .token(token)
                .build();
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 11) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
