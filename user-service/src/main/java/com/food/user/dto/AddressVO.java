package com.food.user.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AddressVO {
    private String addressId;
    private String label;
    private String detail;
    private String contactName;
    private String contactPhone;
    private Boolean isDefault;
}
