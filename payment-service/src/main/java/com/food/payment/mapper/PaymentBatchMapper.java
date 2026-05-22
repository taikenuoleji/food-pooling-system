package com.food.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.food.payment.model.entity.PaymentBatchEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentBatchMapper extends BaseMapper<PaymentBatchEntity> {
}
