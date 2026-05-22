package com.food.pool.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.food.common.constants.BusinessConstants;
import com.food.common.dto.SettlementDTO;
import com.food.common.event.PoolDissolvedEvent;
import com.food.common.event.PoolFormedEvent;
import com.food.common.exception.BusinessException;
import com.food.common.utils.IdGenerator;
import com.food.pool.dto.*;
import com.food.pool.mapper.PoolItemMapper;
import com.food.pool.mapper.PoolMapper;
import com.food.pool.mapper.PoolParticipantMapper;
import com.food.pool.model.entity.PoolEntity;
import com.food.pool.model.entity.PoolItemEntity;
import com.food.pool.model.entity.PoolParticipantEntity;
import com.food.pool.mq.PoolEventProducer;
import com.food.pool.service.PoolFormationService;
import com.food.pool.service.PoolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PoolServiceImpl implements PoolService {

    private final PoolMapper poolMapper;
    private final PoolParticipantMapper participantMapper;
    private final PoolItemMapper itemMapper;
    private final PoolFormationService formationService;
    private final RedissonClient redissonClient;
    private final PoolEventProducer eventProducer;

    @Override
    @Transactional
    public PoolDetailVO createPool(String creatorId, CreatePoolRequest request) {
        PoolEntity pool = new PoolEntity();
        pool.setPoolId(IdGenerator.generate(BusinessConstants.ID_PREFIX_POOL));
        pool.setInviteCode(generateInviteCode());
        pool.setCreatorId(creatorId);
        pool.setMerchantId(request.getMerchantId());
        pool.setMerchantName(request.getMerchantName());
        pool.setStatus(BusinessConstants.POOL_STATUS_CREATED);

        FormationRule rule = request.getFormationRule();
        pool.setMinMembers(rule != null && rule.getMinMembers() != null ? rule.getMinMembers() : 2);
        pool.setMinAmount(rule != null && rule.getMinAmount() != null ? rule.getMinAmount() : 0L);
        pool.setDeadlineMinutes(rule != null && rule.getDeadlineMinutes() != null ? rule.getDeadlineMinutes() : 30);

        pool.setCurrentMembers(0);
        pool.setCurrentFoodAmount(0L);
        pool.setRemark(request.getRemark());
        pool.setExpiresAt(LocalDateTime.now().plusMinutes(pool.getDeadlineMinutes()));
        pool.setVersion(0);
        poolMapper.insert(pool);

        // 发起者自动作为第一个参与者加入
        PoolParticipantEntity creator = new PoolParticipantEntity();
        creator.setParticipantId(IdGenerator.generate(BusinessConstants.ID_PREFIX_PARTICIPANT));
        creator.setPoolId(pool.getPoolId());
        creator.setUserId(creatorId);
        creator.setRole("CREATOR");
        creator.setAddressId(request.getCreatorAddressId());
        creator.setFoodAmount(0L);
        creator.setStatus(BusinessConstants.PARTICIPANT_ACTIVE);
        creator.setJoinedAt(LocalDateTime.now());
        participantMapper.insert(creator);

        // 更新拼单状态为 FORMING
        pool.setCurrentMembers(1);
        pool.setStatus(BusinessConstants.POOL_STATUS_FORMING);
        poolMapper.updateById(pool);

        return buildPoolDetailVO(pool);
    }

    @Override
    @Transactional
    public PoolDetailVO joinPool(String poolId, String userId, JoinPoolRequest request) {
        RLock lock = redissonClient.getLock("lock:pool:" + poolId);
        try {
            if (!lock.tryLock(3, 5, java.util.concurrent.TimeUnit.SECONDS)) {
                throw new BusinessException(BusinessConstants.CODE_POOL_CONFLICT, "系统繁忙，请稍后重试");
            }

            PoolEntity pool = poolMapper.selectOne(
                    new LambdaQueryWrapper<PoolEntity>().eq(PoolEntity::getPoolId, poolId));
            if (pool == null) {
                throw new BusinessException(BusinessConstants.CODE_NOT_FOUND, "拼单不存在");
            }
            if (!BusinessConstants.POOL_STATUS_FORMING.equals(pool.getStatus())
                    && !BusinessConstants.POOL_STATUS_CREATED.equals(pool.getStatus())) {
                throw new BusinessException(BusinessConstants.CODE_POOL_CONFLICT, "拼单已不在进行中");
            }

            // 检查是否已加入
            PoolParticipantEntity existing = participantMapper.selectOne(
                    new LambdaQueryWrapper<PoolParticipantEntity>()
                            .eq(PoolParticipantEntity::getPoolId, poolId)
                            .eq(PoolParticipantEntity::getUserId, userId));
            if (existing != null && BusinessConstants.PARTICIPANT_ACTIVE.equals(existing.getStatus())) {
                throw new BusinessException(BusinessConstants.CODE_POOL_CONFLICT, "已加入该拼单");
            }

            // 计算菜品金额
            long foodAmount = 0;
            if (request.getItems() != null) {
                for (JoinPoolRequest.ItemRequest item : request.getItems()) {
                    foodAmount += item.getUnitPrice() * item.getQuantity();
                }
            }

            // 插入参与者
            PoolParticipantEntity participant = new PoolParticipantEntity();
            participant.setParticipantId(IdGenerator.generate(BusinessConstants.ID_PREFIX_PARTICIPANT));
            participant.setPoolId(poolId);
            participant.setUserId(userId);
            participant.setRole("MEMBER");
            participant.setAddressId(request.getAddressId());
            participant.setFoodAmount(foodAmount);
            participant.setStatus(BusinessConstants.PARTICIPANT_ACTIVE);
            participant.setRemark(request.getRemark());
            participant.setJoinedAt(LocalDateTime.now());
            participantMapper.insert(participant);

            // 插入菜品明细
            if (request.getItems() != null) {
                for (JoinPoolRequest.ItemRequest item : request.getItems()) {
                    PoolItemEntity poolItem = new PoolItemEntity();
                    poolItem.setPoolId(poolId);
                    poolItem.setParticipantId(participant.getParticipantId());
                    poolItem.setUserId(userId);
                    poolItem.setItemId(item.getItemId());
                    poolItem.setItemName(item.getItemName());
                    poolItem.setUnitPrice(item.getUnitPrice());
                    poolItem.setQuantity(item.getQuantity());
                    poolItem.setTotalPrice(item.getUnitPrice() * item.getQuantity());
                    itemMapper.insert(poolItem);
                }
            }

            // 更新拼单统计（CAS乐观锁）
            int affected = poolMapper.update(null, new LambdaUpdateWrapper<PoolEntity>()
                    .eq(PoolEntity::getPoolId, poolId)
                    .eq(PoolEntity::getVersion, pool.getVersion())
                    .setSql("current_members = current_members + 1")
                    .setSql("current_food_amount = current_food_amount + " + foodAmount)
                    .setSql("version = version + 1"));

            if (affected == 0) {
                throw new BusinessException(BusinessConstants.CODE_POOL_CONFLICT, "并发冲突，请重试");
            }

            // 重新加载拼单
            pool = poolMapper.selectOne(
                    new LambdaQueryWrapper<PoolEntity>().eq(PoolEntity::getPoolId, poolId));

            // 检查成团条件
            if (formationService.shouldForm(pool)) {
                // 超时解散
                if (LocalDateTime.now().isAfter(pool.getExpiresAt())) {
                    dissolvePool(pool, "POOL_DISSOLVED_TIMEOUT");
                } else {
                    // 成团
                    formPool(pool);
                }
            }

            return buildPoolDetailVO(pool);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(BusinessConstants.CODE_SERVER_ERROR, "系统繁忙");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    @Transactional
    public void leavePool(String poolId, String userId) {
        PoolEntity pool = poolMapper.selectOne(
                new LambdaQueryWrapper<PoolEntity>().eq(PoolEntity::getPoolId, poolId));
        if (pool == null) {
            throw new BusinessException(BusinessConstants.CODE_NOT_FOUND, "拼单不存在");
        }
        if (!BusinessConstants.POOL_STATUS_FORMING.equals(pool.getStatus())) {
            throw new BusinessException(BusinessConstants.CODE_POOL_CONFLICT, "拼单已不在进行中，无法退出");
        }

        PoolParticipantEntity participant = participantMapper.selectOne(
                new LambdaQueryWrapper<PoolParticipantEntity>()
                        .eq(PoolParticipantEntity::getPoolId, poolId)
                        .eq(PoolParticipantEntity::getUserId, userId)
                        .eq(PoolParticipantEntity::getStatus, BusinessConstants.PARTICIPANT_ACTIVE));
        if (participant == null) {
            throw new BusinessException(BusinessConstants.CODE_NOT_FOUND, "未参与该拼单");
        }

        // 标记为已退出
        participant.setStatus(BusinessConstants.PARTICIPANT_LEFT);
        participant.setLeftAt(LocalDateTime.now());
        participantMapper.updateById(participant);

        // 更新拼单统计
        long foodAmount = participant.getFoodAmount();
        poolMapper.update(null, new LambdaUpdateWrapper<PoolEntity>()
                .eq(PoolEntity::getPoolId, poolId)
                .setSql("current_members = current_members - 1")
                .setSql("current_food_amount = current_food_amount - " + foodAmount));

        // 如果是发起者退出，或剩余活跃人数<2，解散拼单
        PoolEntity updatedPool = poolMapper.selectOne(
                new LambdaQueryWrapper<PoolEntity>().eq(PoolEntity::getPoolId, poolId));
        if ("CREATOR".equals(participant.getRole()) || updatedPool.getCurrentMembers() < 2) {
            dissolvePool(updatedPool, "POOL_DISSOLVED_LAST_LEAVE");
        }
    }

    @Override
    public PoolDetailVO getPoolDetail(String poolId) {
        PoolEntity pool = poolMapper.selectOne(
                new LambdaQueryWrapper<PoolEntity>().eq(PoolEntity::getPoolId, poolId));
        if (pool == null) {
            throw new BusinessException(BusinessConstants.CODE_NOT_FOUND, "拼单不存在");
        }
        return buildPoolDetailVO(pool);
    }

    @Override
    public List<PoolListItemVO> listMyPools(String userId, String status, int page, int size) {
        // 先查出用户参与的拼单ID
        LambdaQueryWrapper<PoolParticipantEntity> wrapper = new LambdaQueryWrapper<PoolParticipantEntity>()
                .eq(PoolParticipantEntity::getUserId, userId)
                .eq(PoolParticipantEntity::getStatus, BusinessConstants.PARTICIPANT_ACTIVE)
                .orderByDesc(PoolParticipantEntity::getJoinedAt);

        List<PoolParticipantEntity> participations = participantMapper.selectList(wrapper);

        if (participations.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> poolIds = participations.stream()
                .map(PoolParticipantEntity::getPoolId)
                .collect(Collectors.toList());

        LambdaQueryWrapper<PoolEntity> poolWrapper = new LambdaQueryWrapper<PoolEntity>()
                .in(PoolEntity::getPoolId, poolIds)
                .orderByDesc(PoolEntity::getCreatedAt);

        if (status != null && !status.isEmpty()) {
            poolWrapper.eq(PoolEntity::getStatus, status);
        }

        poolWrapper.last("LIMIT " + (page - 1) * size + "," + size);
        List<PoolEntity> pools = poolMapper.selectList(poolWrapper);

        Map<String, PoolParticipantEntity> participationMap = participations.stream()
                .collect(Collectors.toMap(PoolParticipantEntity::getPoolId, p -> p));

        return pools.stream().map(p -> {
            PoolParticipantEntity part = participationMap.get(p.getPoolId());
            return PoolListItemVO.builder()
                    .poolId(p.getPoolId())
                    .merchantName(p.getMerchantName())
                    .status(p.getStatus())
                    .role(part != null ? part.getRole() : "MEMBER")
                    .mySubtotal(part != null ? part.getFoodAmount() : 0L)
                    .joinedAt(part != null ? part.getJoinedAt() : p.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    private PoolDetailVO buildPoolDetailVO(PoolEntity pool) {
        // 查询参与者
        List<PoolParticipantEntity> participants = participantMapper.selectList(
                new LambdaQueryWrapper<PoolParticipantEntity>()
                        .eq(PoolParticipantEntity::getPoolId, pool.getPoolId())
                        .eq(PoolParticipantEntity::getStatus, BusinessConstants.PARTICIPANT_ACTIVE)
                        .orderByAsc(PoolParticipantEntity::getJoinedAt));

        List<ParticipantVO> participantVOs = participants.stream().map(p -> {
            // 查询该参与者的菜品
            List<PoolItemEntity> items = itemMapper.selectList(
                    new LambdaQueryWrapper<PoolItemEntity>()
                            .eq(PoolItemEntity::getPoolId, pool.getPoolId())
                            .eq(PoolItemEntity::getParticipantId, p.getParticipantId()));

            List<ParticipantVO.ItemVO> itemVOs = items.stream().map(i ->
                    ParticipantVO.ItemVO.builder()
                            .itemName(i.getItemName())
                            .unitPrice(i.getUnitPrice())
                            .quantity(i.getQuantity())
                            .build()
            ).collect(Collectors.toList());

            return ParticipantVO.builder()
                    .userId(p.getUserId())
                    .nickname("") // 需要通过Feign获取，简化处理
                    .items(itemVOs)
                    .subtotal(p.getFoodAmount())
                    .joinedAt(p.getJoinedAt())
                    .build();
        }).collect(Collectors.toList());

        FormationRule rule = new FormationRule();
        rule.setMinMembers(pool.getMinMembers());
        rule.setMinAmount(pool.getMinAmount());
        rule.setDeadlineMinutes(pool.getDeadlineMinutes());

        return PoolDetailVO.builder()
                .poolId(pool.getPoolId())
                .merchantName(pool.getMerchantName())
                .status(pool.getStatus())
                .formationRule(rule)
                .currentMembers(pool.getCurrentMembers())
                .currentTotalAmount(pool.getCurrentFoodAmount())
                .createdAt(pool.getCreatedAt())
                .expiresAt(pool.getExpiresAt())
                .participants(participantVOs)
                .build();
    }

    private void formPool(PoolEntity pool) {
        log.info("拼单 {} 成团！", pool.getPoolId());
        pool.setStatus(BusinessConstants.POOL_STATUS_FORMED);
        pool.setFormedAt(LocalDateTime.now());
        poolMapper.updateById(pool);

        // 构造并发送成团事件
        List<PoolParticipantEntity> participants = participantMapper.selectList(
                new LambdaQueryWrapper<PoolParticipantEntity>()
                        .eq(PoolParticipantEntity::getPoolId, pool.getPoolId())
                        .eq(PoolParticipantEntity::getStatus, BusinessConstants.PARTICIPANT_ACTIVE));

        List<String> userIds = participants.stream().map(PoolParticipantEntity::getUserId).collect(Collectors.toList());
        List<Long> foodAmounts = participants.stream().map(PoolParticipantEntity::getFoodAmount).collect(Collectors.toList());

        // 计算费用分摊
        List<SettlementDTO> settlements = calculateSettlement(userIds, foodAmounts,
                pool.getDeliveryFee() != null ? pool.getDeliveryFee() : 0L,
                pool.getPackagingFee() != null ? pool.getPackagingFee() : 0L);

        PoolFormedEvent event = new PoolFormedEvent();
        event.setPoolId(pool.getPoolId());
        event.setMerchantId(pool.getMerchantId());
        event.setMerchantName(pool.getMerchantName());
        event.setMemberCount(pool.getCurrentMembers());
        event.setDeliveryFee(pool.getDeliveryFee() != null ? pool.getDeliveryFee() : 0L);
        event.setPackagingFee(pool.getPackagingFee() != null ? pool.getPackagingFee() : 0L);

        List<PoolFormedEvent.Participant> eventParticipants = new ArrayList<>();
        for (SettlementDTO s : settlements) {
            PoolFormedEvent.Participant p = new PoolFormedEvent.Participant();
            p.setUserId(s.getUserId());
            p.setFoodAmount(s.getFoodAmount());
            p.setDeliveryShare(s.getDeliveryShare());
            p.setPackagingShare(s.getPackagingShare());
            p.setTotalAmount(s.getTotalAmount());
            eventParticipants.add(p);
        }
        event.setParticipants(eventParticipants);

        eventProducer.sendPoolFormed(event);
    }

    private void dissolvePool(PoolEntity pool, String reason) {
        log.info("拼单 {} 解散，原因: {}", pool.getPoolId(), reason);
        pool.setStatus(BusinessConstants.POOL_STATUS_DISSOLVED);
        pool.setDissolvedAt(LocalDateTime.now());
        pool.setDissolveReason(reason);
        poolMapper.updateById(pool);

        PoolDissolvedEvent event = new PoolDissolvedEvent();
        event.setPoolId(pool.getPoolId());
        event.setReason(reason);
        eventProducer.sendPoolDissolved(event);
    }

    /**
     * 按人头均摊公共费用
     */
    private List<SettlementDTO> calculateSettlement(List<String> userIds, List<Long> foodAmounts,
                                                     long deliveryFee, long packagingFee) {
        int count = userIds.size();
        if (count == 0) return Collections.emptyList();

        long deliveryEach = deliveryFee / count;
        long deliveryRemainder = deliveryFee - deliveryEach * count;
        long packagingEach = packagingFee / count;
        long packagingRemainder = packagingFee - packagingEach * count;

        List<SettlementDTO> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            long dShare = deliveryEach + (i == count - 1 ? deliveryRemainder : 0);
            long pShare = packagingEach + (i == count - 1 ? packagingRemainder : 0);
            result.add(SettlementDTO.builder()
                    .userId(userIds.get(i))
                    .foodAmount(foodAmounts.get(i))
                    .deliveryShare(dShare)
                    .packagingShare(pShare)
                    .couponShare(0L)
                    .totalAmount(foodAmounts.get(i) + dShare + pShare)
                    .build());
        }
        return result;
    }

    @Override
    public List<PoolListItemVO> listAvailablePools(String userId, int page, int size) {
        // 查询用户已参与的拼单ID
        List<String> joinedPoolIds = participantMapper.selectList(
                new LambdaQueryWrapper<PoolParticipantEntity>()
                        .eq(PoolParticipantEntity::getUserId, userId)
                        .eq(PoolParticipantEntity::getStatus, BusinessConstants.PARTICIPANT_ACTIVE))
                .stream().map(PoolParticipantEntity::getPoolId).collect(Collectors.toList());

        LambdaQueryWrapper<PoolEntity> wrapper = new LambdaQueryWrapper<PoolEntity>()
                .in(PoolEntity::getStatus, BusinessConstants.POOL_STATUS_FORMING, BusinessConstants.POOL_STATUS_CREATED)
                .gt(PoolEntity::getExpiresAt, LocalDateTime.now())
                .orderByDesc(PoolEntity::getCreatedAt);

        if (!joinedPoolIds.isEmpty()) {
            wrapper.notIn(PoolEntity::getPoolId, joinedPoolIds);
        }

        wrapper.last("LIMIT " + (page - 1) * size + "," + size);
        List<PoolEntity> pools = poolMapper.selectList(wrapper);

        return pools.stream().map(p -> PoolListItemVO.builder()
                .poolId(p.getPoolId())
                .merchantName(p.getMerchantName())
                .status(p.getStatus())
                .role(null)
                .mySubtotal(0L)
                .joinedAt(p.getCreatedAt())
                .build()).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PoolDetailVO addItems(String poolId, String userId, List<JoinPoolRequest.ItemRequest> items) {
        PoolEntity pool = poolMapper.selectOne(
                new LambdaQueryWrapper<PoolEntity>().eq(PoolEntity::getPoolId, poolId));
        if (pool == null) {
            throw new BusinessException(BusinessConstants.CODE_NOT_FOUND, "拼单不存在");
        }
        if (!BusinessConstants.POOL_STATUS_FORMING.equals(pool.getStatus())
                && !BusinessConstants.POOL_STATUS_CREATED.equals(pool.getStatus())) {
            throw new BusinessException(BusinessConstants.CODE_POOL_CONFLICT, "拼单已不在进行中");
        }

        PoolParticipantEntity participant = participantMapper.selectOne(
                new LambdaQueryWrapper<PoolParticipantEntity>()
                        .eq(PoolParticipantEntity::getPoolId, poolId)
                        .eq(PoolParticipantEntity::getUserId, userId)
                        .eq(PoolParticipantEntity::getStatus, BusinessConstants.PARTICIPANT_ACTIVE));
        if (participant == null) {
            throw new BusinessException(BusinessConstants.CODE_NOT_FOUND, "未参与该拼单");
        }

        // 计算新增菜品金额
        long foodAmount = 0;
        if (items != null) {
            for (JoinPoolRequest.ItemRequest item : items) {
                PoolItemEntity poolItem = new PoolItemEntity();
                poolItem.setPoolId(poolId);
                poolItem.setParticipantId(participant.getParticipantId());
                poolItem.setUserId(userId);
                poolItem.setItemId(item.getItemId());
                poolItem.setItemName(item.getItemName());
                poolItem.setUnitPrice(item.getUnitPrice());
                poolItem.setQuantity(item.getQuantity());
                poolItem.setTotalPrice(item.getUnitPrice() * item.getQuantity());
                itemMapper.insert(poolItem);
                foodAmount += item.getUnitPrice() * item.getQuantity();
            }
        }

        // 更新参与者金额
        participant.setFoodAmount(participant.getFoodAmount() + foodAmount);
        participantMapper.updateById(participant);

        // CAS 更新拼单统计
        int affected = poolMapper.update(null, new LambdaUpdateWrapper<PoolEntity>()
                .eq(PoolEntity::getPoolId, poolId)
                .eq(PoolEntity::getVersion, pool.getVersion())
                .setSql("current_food_amount = current_food_amount + " + foodAmount)
                .setSql("version = version + 1"));

        if (affected == 0) {
            throw new BusinessException(BusinessConstants.CODE_POOL_CONFLICT, "并发冲突，请重试");
        }

        pool = poolMapper.selectOne(
                new LambdaQueryWrapper<PoolEntity>().eq(PoolEntity::getPoolId, poolId));

        if (formationService.shouldForm(pool)) {
            if (LocalDateTime.now().isAfter(pool.getExpiresAt())) {
                dissolvePool(pool, "POOL_DISSOLVED_TIMEOUT");
            } else {
                formPool(pool);
            }
        }

        return buildPoolDetailVO(pool);
    }

    private String generateInviteCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
