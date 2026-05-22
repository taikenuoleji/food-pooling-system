-- ============================================
-- 支付服务数据库初始化脚本
-- 数据库名: pay_db
-- ============================================

CREATE DATABASE IF NOT EXISTS pay_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE pay_db;

-- 支付批次表
CREATE TABLE IF NOT EXISTS payment_batches (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    batch_id        VARCHAR(20)     NOT NULL COMMENT '批次ID',
    pool_id         VARCHAR(20)     NOT NULL COMMENT '拼单ID',
    order_id        VARCHAR(20)     NOT NULL COMMENT '订单ID',
    batch_type      VARCHAR(10)     NOT NULL COMMENT 'CHARGE/REFUND',
    total_amount    BIGINT          NOT NULL COMMENT '批次总金额（分）',
    status          VARCHAR(15)     NOT NULL COMMENT 'PROCESSING/SUCCESS/PARTIAL_SUCCESS/FAILED',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_batch_id (batch_id),
    INDEX idx_pool_id (pool_id),
    INDEX idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付批次表';

-- 支付记录表
CREATE TABLE IF NOT EXISTS payment_records (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    record_id           VARCHAR(20)     NOT NULL COMMENT '记录ID',
    batch_id            VARCHAR(20)     NOT NULL COMMENT '批次ID',
    user_id             VARCHAR(20)     NOT NULL COMMENT '用户ID',
    pool_id             VARCHAR(20)     NOT NULL COMMENT '拼单ID',
    order_id            VARCHAR(20)     DEFAULT NULL COMMENT '订单ID',
    type                VARCHAR(10)     NOT NULL COMMENT '类型：CHARGE/REFUND',
    amount              BIGINT          NOT NULL COMMENT '金额（分）',
    food_amount         BIGINT          NOT NULL DEFAULT 0 COMMENT '菜品分摊（分）',
    delivery_share      BIGINT          NOT NULL DEFAULT 0 COMMENT '配送费分摊（分）',
    packaging_share     BIGINT          NOT NULL DEFAULT 0 COMMENT '包装费分摊（分）',
    coupon_share        BIGINT          NOT NULL DEFAULT 0 COMMENT '优惠券分摊（分）',
    pay_channel         VARCHAR(20)     DEFAULT NULL COMMENT '支付渠道：WECHAT/ALIPAY/BALANCE',
    channel_trade_no    VARCHAR(64)     DEFAULT NULL COMMENT '第三方交易流水号',
    status              VARCHAR(10)     NOT NULL COMMENT 'PENDING/SUCCESS/FAILED',
    fail_reason         VARCHAR(200)    DEFAULT NULL COMMENT '失败原因',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_record_id (record_id),
    INDEX idx_batch_id (batch_id),
    INDEX idx_user_id (user_id),
    INDEX idx_pool_id (pool_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付记录表';
