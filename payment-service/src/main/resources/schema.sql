-- 支付批次表
CREATE TABLE IF NOT EXISTS payment_batches (
    id              BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    batch_id        VARCHAR(20)     NOT NULL,
    pool_id         VARCHAR(20)     NOT NULL,
    order_id        VARCHAR(20),
    batch_type      VARCHAR(20)     NOT NULL,
    total_amount    BIGINT          NOT NULL DEFAULT 0,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_batch_id UNIQUE (batch_id)
);

-- 支付记录表
CREATE TABLE IF NOT EXISTS payment_records (
    id              BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    record_id       VARCHAR(20)     NOT NULL,
    batch_id        VARCHAR(20)     NOT NULL,
    user_id         VARCHAR(20)     NOT NULL,
    pool_id         VARCHAR(20),
    order_id        VARCHAR(20),
    type            VARCHAR(20)     DEFAULT 'CHARGE',
    amount          BIGINT          NOT NULL DEFAULT 0,
    food_amount     BIGINT          DEFAULT 0,
    delivery_share  BIGINT          DEFAULT 0,
    packaging_share BIGINT          DEFAULT 0,
    coupon_share    BIGINT          DEFAULT 0,
    pay_channel     VARCHAR(50),
    channel_trade_no VARCHAR(100),
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    fail_reason     VARCHAR(200),
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_record_id UNIQUE (record_id)
);
