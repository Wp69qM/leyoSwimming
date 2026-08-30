CREATE TABLE IF NOT EXISTS `order` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(32) NOT NULL UNIQUE,
    type VARCHAR(20) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'pending_payment',
    user_id BIGINT NOT NULL,
    coach_id BIGINT DEFAULT NULL,
    package_id BIGINT DEFAULT NULL,
    purchase_order_id BIGINT DEFAULT NULL,
    original_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    paid_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    payment_method VARCHAR(20) DEFAULT NULL,
    channel_trade_no VARCHAR(64) DEFAULT NULL,
    reason VARCHAR(500) DEFAULT NULL,
    rejected_reason VARCHAR(500) DEFAULT NULL,
    approved_by BIGINT DEFAULT NULL,
    approved_at DATETIME DEFAULT NULL,
    refunded_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_order_user_id ON `order` (user_id);
CREATE INDEX idx_order_coach_id ON `order` (coach_id);
CREATE INDEX idx_order_package_id ON `order` (package_id);
CREATE INDEX idx_order_status_created ON `order` (status, created_at DESC);
CREATE INDEX idx_order_type ON `order` (type);
CREATE INDEX idx_order_purchase_order_id ON `order` (purchase_order_id);

CREATE TABLE IF NOT EXISTS payment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    channel VARCHAR(20) NOT NULL,
    channel_trade_no VARCHAR(64) DEFAULT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    paid_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_payment_order_id ON payment (order_id);
CREATE INDEX idx_payment_channel_trade_no ON payment (channel_trade_no);

CREATE TABLE IF NOT EXISTS refund_transaction (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    refund_record_id BIGINT DEFAULT NULL,
    channel VARCHAR(20) NOT NULL,
    channel_refund_no VARCHAR(64) DEFAULT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    failure_reason VARCHAR(500) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_refund_transaction_order_id ON refund_transaction (order_id);
CREATE INDEX idx_refund_transaction_refund_record_id ON refund_transaction (refund_record_id);

ALTER TABLE refund_record ADD COLUMN order_id BIGINT DEFAULT NULL;
ALTER TABLE refund_record ADD COLUMN reason VARCHAR(500) DEFAULT NULL;

CREATE INDEX idx_refund_record_order_id ON refund_record (order_id);
