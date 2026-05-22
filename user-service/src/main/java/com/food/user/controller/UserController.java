package com.food.user.controller;

import com.food.common.dto.UserInfoDTO;
import com.food.common.result.Result;
import com.food.user.dto.LoginRequest;
import com.food.user.dto.RegisterRequest;
import com.food.user.dto.UserVO;
import com.food.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public Result<UserVO> register(@RequestBody RegisterRequest request) {
        return Result.success(userService.register(request));
    }

    @PostMapping("/login")
    public Result<UserVO> login(@RequestBody LoginRequest request) {
        return Result.success(userService.login(request));
    }

    @GetMapping("/{userId}")
    public Result<UserVO> getUserById(@PathVariable String userId) {
        return Result.success(userService.getUserById(userId));
    }

    /**
     * 内部接口：供其他服务调用
     */
    @GetMapping("/{userId}/info")
    public Result<UserInfoDTO> getUserInfo(@PathVariable String userId) {
        return Result.success(userService.getUserInfo(userId));
    }
}
