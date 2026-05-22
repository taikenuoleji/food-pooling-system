package com.food.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.food.order.model.entity.OrderEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper extends BaseMapper<OrderEntity> {
}
