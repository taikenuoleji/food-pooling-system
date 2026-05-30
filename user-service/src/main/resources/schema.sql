-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id              BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id         VARCHAR(20)     NOT NULL,
    phone           VARCHAR(20)     NOT NULL,
    nickname        VARCHAR(50)     NOT NULL,
    avatar_url      VARCHAR(255)    DEFAULT NULL,
    status          TINYINT         NOT NULL DEFAULT 1,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_id UNIQUE (user_id),
    CONSTRAINT uk_phone UNIQUE (phone)
);

-- 用户地址表
CREATE TABLE IF NOT EXISTS user_addresses (
    id              BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    address_id      VARCHAR(20)     NOT NULL,
    user_id         VARCHAR(20)     NOT NULL,
    label           VARCHAR(20)     DEFAULT NULL,
    detail_address  VARCHAR(200)    NOT NULL,
    contact_name    VARCHAR(50)     NOT NULL,
    contact_phone   VARCHAR(20)     NOT NULL,
    is_default      TINYINT         NOT NULL DEFAULT 0,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_address_id UNIQUE (address_id)
);
