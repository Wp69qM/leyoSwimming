CREATE TABLE IF NOT EXISTS admin_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL COMMENT '管理员用户名',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希（bcrypt）',
    name VARCHAR(64) NOT NULL COMMENT '显示名称',
    role VARCHAR(32) NOT NULL DEFAULT 'admin' COMMENT '角色：admin/super_admin',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-启用，1-禁用',
    last_login_at DATETIME DEFAULT NULL COMMENT '最后登录时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_admin_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员账号表';

CREATE TABLE IF NOT EXISTS admin_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    admin_user_id BIGINT NOT NULL COMMENT '管理员 ID',
    token_hash VARCHAR(255) NOT NULL COMMENT 'token 哈希（SHA-256）',
    expires_at DATETIME NOT NULL COMMENT '过期时间',
    revoked_at DATETIME DEFAULT NULL COMMENT '失效时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_admin_session_token (token_hash),
    KEY idx_admin_session_admin_user_id (admin_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员会话表';

CREATE TABLE IF NOT EXISTS admin_login_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    admin_user_id BIGINT DEFAULT NULL COMMENT '管理员 ID（失败时可能为 NULL）',
    username VARCHAR(64) NOT NULL COMMENT '登录用户名',
    ip VARCHAR(64) DEFAULT NULL COMMENT '登录 IP',
    user_agent VARCHAR(512) DEFAULT NULL COMMENT '浏览器 User-Agent',
    status TINYINT NOT NULL COMMENT '状态：0-成功，1-失败',
    reason VARCHAR(255) DEFAULT NULL COMMENT '失败原因',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_admin_login_log_username (username),
    KEY idx_admin_login_log_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员登录日志表';
