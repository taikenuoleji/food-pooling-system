package com.food.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.food.user.model.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
}
