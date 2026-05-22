package com.food.user.service;

import com.food.common.dto.UserInfoDTO;
import com.food.user.dto.LoginRequest;
import com.food.user.dto.RegisterRequest;
import com.food.user.dto.UserVO;

public interface UserService {

    UserVO register(RegisterRequest request);

    UserVO login(LoginRequest request);

    UserVO getUserById(String userId);

    UserInfoDTO getUserInfo(String userId);
}
