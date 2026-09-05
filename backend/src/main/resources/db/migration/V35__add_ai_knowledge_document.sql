CREATE TABLE IF NOT EXISTS ai_knowledge_document (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL COMMENT '文档标题',
    category VARCHAR(50) NOT NULL COMMENT '分类：safety/technique/emergency/other',
    content_type VARCHAR(20) NOT NULL COMMENT '内容类型：text/markdown',
    source_type VARCHAR(20) NOT NULL COMMENT '来源：manual/file',
    content LONGTEXT NOT NULL COMMENT '文档内容',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-启用，1-禁用',
    created_by BIGINT NOT NULL COMMENT '创建人 ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_knowledge_title (title),
    KEY idx_knowledge_category (category),
    KEY idx_knowledge_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 知识库文档表';
