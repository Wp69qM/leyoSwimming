CREATE TABLE IF NOT EXISTS ai_chat_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL UNIQUE COMMENT '业务会话ID',
    user_id BIGINT COMMENT '登录用户ID，游客为NULL',
    title VARCHAR(64) COMMENT '会话标题，取首条用户消息前20字',
    status TINYINT DEFAULT 0 COMMENT '0 active 1 archived',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_session_user (user_id, status, updated_at)
) COMMENT='AI助理会话';

CREATE TABLE IF NOT EXISTS ai_chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id VARCHAR(64) NOT NULL UNIQUE COMMENT '业务消息ID',
    session_id VARCHAR(64) NOT NULL COMMENT '业务会话ID',
    role VARCHAR(16) NOT NULL COMMENT 'user/assistant/tool',
    content TEXT COMMENT '文本内容',
    recommendations JSON COMMENT '推荐卡片JSON',
    tool_calls JSON COMMENT 'Tool调用记录',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_message_session (session_id, created_at)
) COMMENT='AI助理消息';

CREATE TABLE IF NOT EXISTS ai_recommendation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    message_id VARCHAR(64) NOT NULL,
    user_id BIGINT,
    input TEXT NOT NULL,
    tool_calls JSON,
    llm_response TEXT,
    final_response JSON,
    latency_ms INT,
    llm_latency_ms INT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_log_session (session_id, created_at),
    INDEX idx_log_created (created_at)
) COMMENT='AI推荐日志';
