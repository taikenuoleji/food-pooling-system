package com.food.user.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String phone;
    private String code;
    private String nickname;
}
