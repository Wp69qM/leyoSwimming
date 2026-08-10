CREATE TABLE IF NOT EXISTS coach_resignation_ticket (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coach_id BIGINT NOT NULL COMMENT '教练 ID',
    ticket_no VARCHAR(32) NOT NULL COMMENT '工单号',
    reason VARCHAR(500) DEFAULT NULL COMMENT '离职原因',
    status VARCHAR(20) NOT NULL DEFAULT 'processing' COMMENT 'processing/pending_audit/approved/rejected',
    total_packages INT NOT NULL DEFAULT 0 COMMENT '待处理 active 套餐数',
    handled_packages INT NOT NULL DEFAULT 0 COMMENT '已登记数',
    schedule_cleared TINYINT(1) NOT NULL DEFAULT 0 COMMENT '未来排班是否已清空',
    settlement_status TINYINT NOT NULL DEFAULT 0 COMMENT '0=待结算 1=已结算',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_at DATETIME DEFAULT NULL COMMENT '教练提交至管理员时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ticket_no (ticket_no),
    KEY idx_ticket_coach_id (coach_id),
    KEY idx_ticket_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教练离职工单主表';

CREATE TABLE IF NOT EXISTS coach_resignation_action (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id BIGINT NOT NULL COMMENT '工单 ID',
    package_id BIGINT NOT NULL COMMENT '套餐 ID',
    action VARCHAR(20) NOT NULL COMMENT 'refund/transfer/continue',
    target_coach_id BIGINT DEFAULT NULL COMMENT 'transfer 时新教练 ID',
    status VARCHAR(20) NOT NULL DEFAULT 'registered' COMMENT 'registered/approved/rejected',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_action_ticket_package (ticket_id, package_id),
    KEY idx_action_ticket_id (ticket_id),
    KEY idx_action_package_id (package_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教练离职套餐处理记录表';

CREATE TABLE IF NOT EXISTS refund_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    package_id BIGINT NOT NULL COMMENT '套餐 ID',
    ticket_id BIGINT DEFAULT NULL COMMENT '关联离职工单',
    refund_amount DECIMAL(10,2) NOT NULL COMMENT '退款金额',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0=pending,1=approved,2=rejected,3=completed',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_refund_package_id (package_id),
    KEY idx_refund_ticket_id (ticket_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='退款记录表';
