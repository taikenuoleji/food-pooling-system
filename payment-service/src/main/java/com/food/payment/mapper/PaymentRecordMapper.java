package com.food.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.food.payment.model.entity.PaymentRecordEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentRecordMapper extends BaseMapper<PaymentRecordEntity> {
}
