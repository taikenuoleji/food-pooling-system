-- ============================================
-- 拼单服务数据库初始化脚本
-- 数据库名: pool_db
-- ============================================

CREATE DATABASE IF NOT EXISTS pool_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE pool_db;

-- 拼单主表
CREATE TABLE IF NOT EXISTS pools (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    pool_id             VARCHAR(20)     NOT NULL COMMENT '业务拼单ID',
    invite_code         VARCHAR(8)      NOT NULL COMMENT '邀请码',
    creator_id          VARCHAR(20)     NOT NULL COMMENT '发起者用户ID',
    merchant_id         VARCHAR(20)     NOT NULL COMMENT '商家ID',
    merchant_name       VARCHAR(100)    NOT NULL COMMENT '商家名称',
    status              VARCHAR(15)     NOT NULL COMMENT '状态：CREATED/FORMING/FORMED/ORDERED/DISSOLVED',
    min_members         INT             NOT NULL DEFAULT 2 COMMENT '成团最少人数',
    min_amount          BIGINT          NOT NULL DEFAULT 0 COMMENT '成团最低金额（分）',
    deadline_minutes    INT             NOT NULL DEFAULT 30 COMMENT '拼单有效时长（分钟）',
    current_members     INT             NOT NULL DEFAULT 0 COMMENT '当前参与人数',
    current_food_amount BIGINT          NOT NULL DEFAULT 0 COMMENT '当前菜品总金额（分）',
    order_id            VARCHAR(20)     DEFAULT NULL COMMENT '成团后生成的订单ID',
    delivery_fee        BIGINT          DEFAULT 0 COMMENT '配送费（分）',
    packaging_fee       BIGINT          DEFAULT 0 COMMENT '包装费（分）',
    remark              VARCHAR(500)    DEFAULT NULL COMMENT '备注',
    expires_at          DATETIME        NOT NULL COMMENT '过期时间',
    formed_at           DATETIME        DEFAULT NULL COMMENT '成团时间',
    dissolved_at        DATETIME        DEFAULT NULL COMMENT '解散时间',
    dissolve_reason     VARCHAR(50)     DEFAULT NULL COMMENT '解散原因',
    version             INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_pool_id (pool_id),
    UNIQUE KEY uk_invite_code (invite_code),
    INDEX idx_creator_id (creator_id),
    INDEX idx_status_expires (status, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='拼单主表';

-- 拼单参与者表
CREATE TABLE IF NOT EXISTS pool_participants (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    participant_id  VARCHAR(20)     NOT NULL COMMENT '参与记录ID',
    pool_id         VARCHAR(20)     NOT NULL COMMENT '拼单ID',
    user_id         VARCHAR(20)     NOT NULL COMMENT '用户ID',
    role            VARCHAR(10)     NOT NULL COMMENT '角色：CREATOR/MEMBER',
    address_id      VARCHAR(20)     NOT NULL COMMENT '该用户收货地址ID',
    food_amount     BIGINT          NOT NULL DEFAULT 0 COMMENT '个人菜品金额（分）',
    status          VARCHAR(10)     NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/LEFT/KICKED',
    remark          VARCHAR(200)    DEFAULT NULL COMMENT '个人备注',
    joined_at       DATETIME        NOT NULL COMMENT '加入时间',
    left_at         DATETIME        DEFAULT NULL COMMENT '退出时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_participant_id (participant_id),
    INDEX idx_pool_id (pool_id),
    UNIQUE KEY uk_pool_user (pool_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='拼单参与者表';

-- 拼单菜品明细表
CREATE TABLE IF NOT EXISTS pool_items (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    pool_id         VARCHAR(20)     NOT NULL COMMENT '拼单ID',
    participant_id  VARCHAR(20)     NOT NULL COMMENT '参与者ID',
    user_id         VARCHAR(20)     NOT NULL COMMENT '用户ID',
    item_id         VARCHAR(20)     NOT NULL COMMENT '菜品ID',
    item_name       VARCHAR(100)    NOT NULL COMMENT '菜品名称',
    unit_price      BIGINT          NOT NULL COMMENT '单价（分）',
    quantity        INT             NOT NULL COMMENT '数量',
    total_price     BIGINT          NOT NULL COMMENT '小计（分）',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    INDEX idx_pool_id (pool_id),
    INDEX idx_participant_id (participant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='拼单菜品明细表';
