package com.food.pool.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pool_participants")
public class PoolParticipantEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String participantId;
    private String poolId;
    private String userId;
    private String role;
    private String addressId;
    private Long foodAmount;
    private String status;
    private String remark;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;
}
