package com.food.pool.dto;

import lombok.Data;

@Data
public class CreatePoolRequest {
    private String merchantId;
    private String merchantName;
    private String creatorAddressId;
    private FormationRule formationRule;
    private String remark;
}
