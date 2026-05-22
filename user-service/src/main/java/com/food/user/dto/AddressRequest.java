package com.food.user.dto;

import lombok.Data;

@Data
public class AddressRequest {
    private String label;
    private String detail;
    private String contactName;
    private String contactPhone;
    private Boolean isDefault;
}
