-- 订单表
CREATE TABLE IF NOT EXISTS orders (
    id              BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id        VARCHAR(20)     NOT NULL,
    pool_id         VARCHAR(20)     NOT NULL,
    merchant_id     VARCHAR(20)     NOT NULL,
    merchant_name   VARCHAR(100)    NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING_CONFIRM',
    total_food_amount BIGINT        NOT NULL DEFAULT 0,
    delivery_fee    BIGINT          NOT NULL DEFAULT 0,
    packaging_fee   BIGINT          NOT NULL DEFAULT 0,
    coupon_discount BIGINT          NOT NULL DEFAULT 0,
    total_amount    BIGINT          NOT NULL DEFAULT 0,
    member_count    INT             NOT NULL DEFAULT 1,
    delivery_address VARCHAR(500),
    estimated_arrival TIMESTAMP,
    paid_at         TIMESTAMP,
    confirmed_at    TIMESTAMP,
    completed_at    TIMESTAMP,
    cancelled_at    TIMESTAMP,
    cancel_reason   VARCHAR(200),
    external_order_id VARCHAR(50),
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_order_id UNIQUE (order_id),
    CONSTRAINT uk_pool_id UNIQUE (pool_id)
);

-- 订单菜品表
CREATE TABLE IF NOT EXISTS order_items (
    id              BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id        VARCHAR(20)     NOT NULL,
    item_id         VARCHAR(20)     NOT NULL,
    item_name       VARCHAR(100)    NOT NULL,
    unit_price      BIGINT          NOT NULL,
    total_quantity  INT             NOT NULL DEFAULT 1,
    total_price     BIGINT          NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 订单参与者结算表
CREATE TABLE IF NOT EXISTS order_participant_settlements (
    id              BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    settlement_id   VARCHAR(20)     NOT NULL,
    order_id        VARCHAR(20)     NOT NULL,
    user_id         VARCHAR(20)     NOT NULL,
    food_amount     BIGINT          NOT NULL DEFAULT 0,
    delivery_share  BIGINT          NOT NULL DEFAULT 0,
    packaging_share BIGINT          NOT NULL DEFAULT 0,
    coupon_share    BIGINT          NOT NULL DEFAULT 0,
    total_amount    BIGINT          NOT NULL DEFAULT 0,
    pay_status      VARCHAR(10)     NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_settlement_id UNIQUE (settlement_id)
);
