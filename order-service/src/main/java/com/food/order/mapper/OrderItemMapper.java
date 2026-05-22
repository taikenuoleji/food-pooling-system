package com.food.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.food.order.model.entity.OrderItemEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItemEntity> {
}
