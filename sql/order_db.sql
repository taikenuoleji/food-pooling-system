-- ============================================
-- 订单服务数据库初始化脚本
-- 数据库名: order_db
-- ============================================

CREATE DATABASE IF NOT EXISTS order_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE order_db;

-- 订单主表
CREATE TABLE IF NOT EXISTS orders (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_id            VARCHAR(20)     NOT NULL COMMENT '业务订单ID',
    pool_id             VARCHAR(20)     NOT NULL COMMENT '关联拼单ID',
    merchant_id         VARCHAR(20)     NOT NULL COMMENT '商家ID',
    merchant_name       VARCHAR(100)    NOT NULL COMMENT '商家名称',
    status              VARCHAR(15)     NOT NULL COMMENT 'PENDING_CONFIRM/CONFIRMED/DELIVERING/COMPLETED/CANCELLED',
    total_food_amount   BIGINT          NOT NULL COMMENT '菜品总额（分）',
    delivery_fee        BIGINT          NOT NULL COMMENT '配送费（分）',
    packaging_fee       BIGINT          NOT NULL COMMENT '包装费（分）',
    coupon_discount     BIGINT          NOT NULL DEFAULT 0 COMMENT '优惠减免（分）',
    total_amount        BIGINT          NOT NULL COMMENT '订单总额（分）',
    member_count        INT             NOT NULL COMMENT '参与人数',
    delivery_address    VARCHAR(500)    DEFAULT NULL COMMENT '配送地址（取发起者地址）',
    estimated_arrival   DATETIME        DEFAULT NULL COMMENT '预计送达时间',
    paid_at             DATETIME        DEFAULT NULL COMMENT '支付时间',
    confirmed_at        DATETIME        DEFAULT NULL COMMENT '商家接单时间',
    completed_at        DATETIME        DEFAULT NULL COMMENT '完成时间',
    cancelled_at        DATETIME        DEFAULT NULL COMMENT '取消时间',
    cancel_reason       VARCHAR(200)    DEFAULT NULL COMMENT '取消原因',
    external_order_id   VARCHAR(50)     DEFAULT NULL COMMENT '外部商家/平台订单ID',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_id (order_id),
    UNIQUE KEY uk_pool_id (pool_id),
    INDEX idx_status (status),
    INDEX idx_merchant_id (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单主表';

-- 订单菜品汇总表
CREATE TABLE IF NOT EXISTS order_items (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_id        VARCHAR(20)     NOT NULL COMMENT '订单ID',
    item_id         VARCHAR(20)     NOT NULL COMMENT '菜品ID',
    item_name       VARCHAR(100)    NOT NULL COMMENT '菜品名称',
    unit_price      BIGINT          NOT NULL COMMENT '单价（分）',
    total_quantity  INT             NOT NULL COMMENT '总数量',
    total_price     BIGINT          NOT NULL COMMENT '总价（分）',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    INDEX idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单菜品汇总表';

-- 订单分摊明细表
CREATE TABLE IF NOT EXISTS order_participant_settlements (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_id        VARCHAR(20)     NOT NULL COMMENT '订单ID',
    user_id         VARCHAR(20)     NOT NULL COMMENT '用户ID',
    food_amount     BIGINT          NOT NULL COMMENT '菜品费用（分）',
    delivery_share  BIGINT          NOT NULL COMMENT '配送费分摊（分）',
    packaging_share BIGINT          NOT NULL COMMENT '包装费分摊（分）',
    coupon_share    BIGINT          NOT NULL DEFAULT 0 COMMENT '优惠券分摊（分）',
    total_amount    BIGINT          NOT NULL COMMENT '实付总额（分）',
    pay_status      VARCHAR(10)     NOT NULL COMMENT '支付状态：PENDING/PAID/REFUNDED',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    INDEX idx_order_id (order_id),
    UNIQUE KEY uk_order_user (order_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单分摊明细表';
