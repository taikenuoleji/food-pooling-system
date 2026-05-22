-- ============================================
-- 用户服务数据库初始化脚本
-- 数据库名: user_db
-- ============================================

CREATE DATABASE IF NOT EXISTS user_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE user_db;

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id         VARCHAR(20)     NOT NULL COMMENT '业务用户ID',
    phone           VARCHAR(20)     NOT NULL COMMENT '手机号',
    nickname        VARCHAR(50)     NOT NULL COMMENT '昵称',
    avatar_url      VARCHAR(255)    DEFAULT NULL COMMENT '头像URL',
    status          TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：1-正常 0-禁用',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_id (user_id),
    UNIQUE KEY uk_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 用户地址表
CREATE TABLE IF NOT EXISTS user_addresses (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    address_id      VARCHAR(20)     NOT NULL COMMENT '业务地址ID',
    user_id         VARCHAR(20)     NOT NULL COMMENT '用户ID',
    label           VARCHAR(20)     DEFAULT NULL COMMENT '标签（家/公司/学校）',
    detail_address  VARCHAR(200)    NOT NULL COMMENT '详细地址',
    contact_name    VARCHAR(20)     NOT NULL COMMENT '联系人',
    contact_phone   VARCHAR(20)     NOT NULL COMMENT '联系电话',
    is_default      TINYINT         NOT NULL DEFAULT 0 COMMENT '是否默认地址',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_address_id (address_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户地址表';
