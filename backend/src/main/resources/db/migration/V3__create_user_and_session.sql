-- 替换早期占位 users 表
DROP TABLE IF EXISTS users;

CREATE TABLE IF NOT EXISTS user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    openid VARCHAR(64) NOT NULL COMMENT '微信 openid',
    union_id VARCHAR(64) DEFAULT NULL COMMENT '微信 union_id',
    phone VARCHAR(128) NOT NULL COMMENT 'AES 加密手机号',
    avatar_url VARCHAR(512) DEFAULT NULL COMMENT '头像 URL',
    name VARCHAR(64) DEFAULT NULL COMMENT '微信昵称/姓名',
    identity_status VARCHAR(20) NOT NULL DEFAULT '注册用户' COMMENT '身份状态',
    profile_completed TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已完善资料：0-否，1-是',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0-正常,1-软删除,2-封禁',
    last_login_at DATETIME DEFAULT NULL COMMENT '最后登录时间',
    login_ip VARCHAR(64) DEFAULT NULL COMMENT '最后登录 IP',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_union_id (union_id),
    UNIQUE KEY uk_user_openid (openid),
    UNIQUE KEY uk_user_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户账号表';

CREATE TABLE IF NOT EXISTS user_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    session_key_encrypted VARCHAR(256) NOT NULL COMMENT '加密后的微信 session_key',
    refresh_token_hash VARCHAR(128) NOT NULL COMMENT 'refresh_token 哈希',
    expires_at DATETIME NOT NULL COMMENT '会话过期时间',
    device_name VARCHAR(64) DEFAULT NULL COMMENT '设备名称',
    device_id VARCHAR(128) DEFAULT NULL COMMENT '设备 ID',
    last_active_at DATETIME DEFAULT NULL COMMENT '最后活跃时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_user_session_user_id (user_id),
    UNIQUE KEY uk_user_session_refresh_hash (refresh_token_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户会话表';

CREATE TABLE IF NOT EXISTS user_login_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT DEFAULT NULL COMMENT '用户 ID（失败时可能为 NULL）',
    phone_hash VARCHAR(64) DEFAULT NULL COMMENT '手机号哈希',
    ip VARCHAR(64) DEFAULT NULL COMMENT '登录 IP',
    user_agent VARCHAR(512) DEFAULT NULL COMMENT 'User-Agent',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0-成功，1-失败',
    reason VARCHAR(256) DEFAULT NULL COMMENT '失败原因',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_user_login_log_user_id (user_id),
    KEY idx_user_login_log_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户登录日志表';
