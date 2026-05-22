package com.food.user.service;

import com.food.user.dto.AddressRequest;
import com.food.user.dto.AddressVO;

import java.util.List;

public interface UserAddressService {

    AddressVO addAddress(String userId, AddressRequest request);

    List<AddressVO> listAddresses(String userId);

    String getDefaultAddressId(String userId);
}
