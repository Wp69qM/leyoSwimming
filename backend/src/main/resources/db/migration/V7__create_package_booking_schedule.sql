CREATE TABLE IF NOT EXISTS `package` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    coach_id BIGINT NOT NULL COMMENT '教练 ID',
    total_hours INT NOT NULL COMMENT '总课时',
    consumed_count INT NOT NULL DEFAULT 0 COMMENT '已消耗课时',
    reserved_count INT NOT NULL DEFAULT 0 COMMENT '已预约占用课时',
    available_count INT NOT NULL DEFAULT 0 COMMENT '可用课时',
    price_per_hour DECIMAL(10,2) NOT NULL COMMENT '课时单价',
    paid_amount DECIMAL(10,2) NOT NULL COMMENT '实付金额',
    status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT 'active/exhausted/expired/frozen/refunded',
    frozen_reason VARCHAR(32) DEFAULT NULL COMMENT 'coach_resigned / refund_pending / admin_frozen',
    pending_handover_at DATETIME DEFAULT NULL COMMENT '教练离职标记',
    expire_at DATETIME DEFAULT NULL COMMENT '套餐过期时间',
    exhausted_at DATETIME DEFAULT NULL COMMENT '课时耗尽时间',
    refunded_at DATETIME DEFAULT NULL COMMENT '退款时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_package_user_id (user_id),
    KEY idx_package_coach_id (coach_id),
    KEY idx_package_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户课程套餐表';

CREATE TABLE IF NOT EXISTS booking (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    package_id BIGINT NOT NULL COMMENT '套餐 ID',
    coach_id BIGINT NOT NULL COMMENT '教练 ID',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    start_time DATETIME NOT NULL COMMENT '课程开始时间',
    end_time DATETIME NOT NULL COMMENT '课程结束时间',
    status VARCHAR(20) NOT NULL DEFAULT 'booked' COMMENT 'booked/confirmed/teaching/cancelled/completed',
    cancel_reason TINYINT DEFAULT NULL COMMENT '1=学员取消,2=教练离职,3=学员旷课,4=场馆闭馆,5=教练请假,6=套餐冻结',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_booking_coach_id (coach_id),
    KEY idx_booking_user_id (user_id),
    KEY idx_booking_start_time (start_time),
    KEY idx_booking_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程预约表';

CREATE TABLE IF NOT EXISTS schedule_slot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coach_id BIGINT NOT NULL COMMENT '教练 ID',
    start_time DATETIME NOT NULL COMMENT '时段开始',
    end_time DATETIME NOT NULL COMMENT '时段结束',
    status VARCHAR(20) NOT NULL DEFAULT 'available' COMMENT 'available/booked/hidden',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_schedule_slot_coach_id (coach_id),
    KEY idx_schedule_slot_start_time (start_time),
    KEY idx_schedule_slot_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教练可约时段表';
