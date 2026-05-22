package com.food.user.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserVO {
    private String userId;
    private String phone;
    private String nickname;
    private String avatar;
    private String defaultAddressId;
    private String token;
}
