package com.food.user.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_addresses")
public class UserAddressEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String addressId;
    private String userId;
    private String label;
    private String detailAddress;
    private String contactName;
    private String contactPhone;
    private Integer isDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
