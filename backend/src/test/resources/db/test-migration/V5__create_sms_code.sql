CREATE TABLE IF NOT EXISTS sms_code (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    phone_hash VARCHAR(64) NOT NULL COMMENT '手机号哈希',
    code VARCHAR(8) NOT NULL COMMENT '验证码',
    scene VARCHAR(32) NOT NULL DEFAULT 'login' COMMENT '使用场景',
    app_type VARCHAR(16) NOT NULL DEFAULT 'user' COMMENT '应用类型：user/coach',
    expires_at DATETIME NOT NULL COMMENT '过期时间',
    used TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已使用：0-否，1-是',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_sms_code_phone_scene (phone_hash, scene, app_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='短信验证码表';
