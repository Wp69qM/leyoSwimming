CREATE TABLE IF NOT EXISTS coach (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    openid VARCHAR(64) NOT NULL COMMENT '微信 openid',
    union_id VARCHAR(64) DEFAULT NULL COMMENT '微信 union_id',
    phone VARCHAR(128) NOT NULL COMMENT 'AES 加密手机号',
    avatar_url VARCHAR(512) DEFAULT NULL COMMENT '头像 URL',
    name VARCHAR(64) DEFAULT NULL COMMENT '微信昵称/姓名',
    status SMALLINT NOT NULL DEFAULT -1 COMMENT '-1=未提交,0=待审核,1=已通过,2=已驳回,3=已离职,4=离职中',
    rejection_reason TEXT DEFAULT NULL COMMENT '驳回原因',
    submitted_at DATETIME DEFAULT NULL COMMENT '提交入驻时间',
    last_login_at DATETIME DEFAULT NULL COMMENT '最后登录时间',
    login_ip VARCHAR(64) DEFAULT NULL COMMENT '最后登录 IP',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_coach_union_id (union_id),
    KEY idx_coach_openid (openid),
    KEY idx_coach_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教练账号表';

CREATE TABLE IF NOT EXISTS coach_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coach_id BIGINT NOT NULL COMMENT '教练 ID',
    session_key_encrypted VARCHAR(256) NOT NULL COMMENT '加密后的微信 session_key',
    refresh_token_hash VARCHAR(128) NOT NULL COMMENT 'refresh_token 哈希',
    expires_at DATETIME NOT NULL COMMENT '会话过期时间',
    device_name VARCHAR(64) DEFAULT NULL COMMENT '设备名称',
    device_id VARCHAR(128) DEFAULT NULL COMMENT '设备 ID',
    last_active_at DATETIME DEFAULT NULL COMMENT '最后活跃时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_coach_session_coach_id (coach_id),
    UNIQUE KEY uk_coach_session_refresh_hash (refresh_token_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教练会话表';

CREATE TABLE IF NOT EXISTS coach_login_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coach_id BIGINT DEFAULT NULL COMMENT '教练 ID（失败时可能为 NULL）',
    phone_hash VARCHAR(64) DEFAULT NULL COMMENT '手机号哈希',
    ip VARCHAR(64) DEFAULT NULL COMMENT '登录 IP',
    user_agent VARCHAR(512) DEFAULT NULL COMMENT 'User-Agent',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0-成功，1-失败',
    reason VARCHAR(256) DEFAULT NULL COMMENT '失败原因',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_coach_login_log_coach_id (coach_id),
    KEY idx_coach_login_log_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教练登录日志表';
