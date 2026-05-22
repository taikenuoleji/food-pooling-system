package com.food.common.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserInfoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String userId;
    private String nickname;
    private String phone;
}
