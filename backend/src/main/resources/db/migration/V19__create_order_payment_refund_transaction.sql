CREATE TABLE IF NOT EXISTS `order` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(32) NOT NULL UNIQUE COMMENT '业务单号',
    type VARCHAR(20) NOT NULL COMMENT '订单类型：purchase / refund',
    status VARCHAR(32) NOT NULL DEFAULT 'pending_payment' COMMENT '订单状态',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    coach_id BIGINT DEFAULT NULL COMMENT '教练 ID',
    package_id BIGINT DEFAULT NULL COMMENT '关联套餐 ID',
    purchase_order_id BIGINT DEFAULT NULL COMMENT '退款订单指向原购买订单',
    original_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '应付金额',
    discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '优惠金额',
    paid_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '实付/退款金额',
    payment_method VARCHAR(20) DEFAULT NULL COMMENT '支付方式：wechat / alipay',
    channel_trade_no VARCHAR(64) DEFAULT NULL COMMENT '第三方流水号',
    reason VARCHAR(500) DEFAULT NULL COMMENT '退款申请原因',
    rejected_reason VARCHAR(500) DEFAULT NULL COMMENT '驳回原因',
    approved_by BIGINT DEFAULT NULL COMMENT '处理管理员 ID',
    approved_at DATETIME DEFAULT NULL COMMENT '处理时间',
    refunded_at DATETIME DEFAULT NULL COMMENT '退款成功时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_order_user_id (user_id),
    KEY idx_order_coach_id (coach_id),
    KEY idx_order_package_id (package_id),
    KEY idx_order_status_created (status, created_at DESC),
    KEY idx_order_type (type),
    KEY idx_order_purchase_order_id (purchase_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单主表';

CREATE TABLE IF NOT EXISTS payment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL COMMENT '订单 ID',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    channel VARCHAR(20) NOT NULL COMMENT '支付渠道：wechat / alipay',
    channel_trade_no VARCHAR(64) DEFAULT NULL COMMENT '渠道支付流水号',
    amount DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'pending / success / failed',
    paid_at DATETIME DEFAULT NULL COMMENT '支付成功时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_payment_order_id (order_id),
    KEY idx_payment_channel_trade_no (channel_trade_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付记录表';

CREATE TABLE IF NOT EXISTS refund_transaction (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL COMMENT '退款订单 ID',
    refund_record_id BIGINT DEFAULT NULL COMMENT '退款记录 ID',
    channel VARCHAR(20) NOT NULL COMMENT '退款渠道：wechat / alipay',
    channel_refund_no VARCHAR(64) DEFAULT NULL COMMENT '渠道退款单号',
    amount DECIMAL(10,2) NOT NULL COMMENT '退款金额',
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'pending / success / failed',
    failure_reason VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_refund_transaction_order_id (order_id),
    KEY idx_refund_transaction_refund_record_id (refund_record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='退款渠道流水表';

ALTER TABLE refund_record
    ADD COLUMN order_id BIGINT DEFAULT NULL COMMENT '关联退款订单 ID' AFTER `package_id`,
    ADD COLUMN reason VARCHAR(500) DEFAULT NULL COMMENT '退款原因' AFTER `refund_amount`,
    ADD KEY idx_refund_record_order_id (order_id);
