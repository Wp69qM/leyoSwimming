CREATE TABLE IF NOT EXISTS `package_template` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL COMMENT '套餐名称',
    package_mode VARCHAR(20) NOT NULL DEFAULT 'standard' COMMENT '套餐模式：standard / experience',
    teaching_type VARCHAR(20) NOT NULL DEFAULT 'one_on_one' COMMENT '教学类型：one_on_one / one_on_two / one_on_three',
    stroke_ids VARCHAR(2000) DEFAULT NULL COMMENT '泳姿 ID 列表',
    total_hours INT NOT NULL COMMENT '总课时',
    duration_minutes INT NOT NULL DEFAULT 60 COMMENT '每节课时长（分钟）',
    valid_days INT NOT NULL COMMENT '有效期天数',
    original_price DECIMAL(10,2) NOT NULL COMMENT '原价',
    price DECIMAL(10,2) NOT NULL COMMENT '售价',
    refund_enabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否支持退款',
    refund_ratio DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '退款比例 0-100',
    refund_valid_days INT NOT NULL DEFAULT 0 COMMENT '退款有效期天数',
    tags VARCHAR(2000) DEFAULT NULL COMMENT '标签列表',
    description TEXT DEFAULT NULL COMMENT '套餐描述',
    images VARCHAR(2000) DEFAULT NULL COMMENT '套餐展示图片 URL 列表',
    status VARCHAR(20) NOT NULL DEFAULT 'inactive' COMMENT '状态：active / inactive',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    UNIQUE KEY uk_package_template_name (name),
    KEY idx_package_template_status (status),
    KEY idx_package_template_package_mode (package_mode)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标准套餐模板表';

CREATE TABLE IF NOT EXISTS `package_template_coach` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    package_template_id BIGINT NOT NULL COMMENT '套餐模板 ID',
    coach_id BIGINT NOT NULL COMMENT '教练 ID',
    reference_price_snapshot DECIMAL(10,2) DEFAULT NULL COMMENT '保存时教练参考单价快照',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_package_template_coach (package_template_id, coach_id),
    KEY idx_package_template_coach_template (package_template_id),
    KEY idx_package_template_coach_coach (coach_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='套餐模板与教练关联表';

CREATE TABLE IF NOT EXISTS `custom_package_config` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    min_hours INT NOT NULL COMMENT '最小课时',
    max_hours INT NOT NULL COMMENT '最大课时',
    default_valid_days INT NOT NULL COMMENT '默认有效期天数',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='自定义套餐全局配置表';
