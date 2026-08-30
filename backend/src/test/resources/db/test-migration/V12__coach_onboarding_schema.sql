-- G3 教练入驻/审核/重新入驻相关表结构

-- 扩展 coach 表字段（保留现有 certificates JSON 字段以兼容 G2 资料页）
ALTER TABLE `coach` ADD COLUMN `email` VARCHAR(128) DEFAULT NULL;
ALTER TABLE `coach` ADD COLUMN `wechat_qr_url` VARCHAR(512) DEFAULT NULL;
ALTER TABLE `coach` ADD COLUMN `id_card_no` VARCHAR(128) DEFAULT NULL;
ALTER TABLE `coach` ADD COLUMN `total_students` INT DEFAULT NULL;
ALTER TABLE `coach` ADD COLUMN `total_hours` INT DEFAULT NULL;
ALTER TABLE `coach` ADD COLUMN `teaching_strokes` VARCHAR(128) DEFAULT NULL;
ALTER TABLE `coach` ADD COLUMN `bio` VARCHAR(500) DEFAULT NULL;
ALTER TABLE `coach` ADD COLUMN `reference_price` DECIMAL(10, 2) DEFAULT NULL;
ALTER TABLE `coach` ADD COLUMN `approved_at` DATETIME DEFAULT NULL;

-- 教练入驻申请快照表
CREATE TABLE IF NOT EXISTS coach_application (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coach_id BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL,
    previous_coach_status SMALLINT DEFAULT NULL,
    name VARCHAR(64) DEFAULT NULL,
    phone VARCHAR(128) DEFAULT NULL,
    gender VARCHAR(10) DEFAULT NULL,
    age INT DEFAULT NULL,
    email VARCHAR(128) DEFAULT NULL,
    wechat_qr_url VARCHAR(512) DEFAULT NULL,
    id_card_no VARCHAR(128) DEFAULT NULL,
    teaching_years INT DEFAULT NULL,
    total_students INT DEFAULT NULL,
    total_hours INT DEFAULT NULL,
    teaching_strokes VARCHAR(128) DEFAULT NULL,
    bio VARCHAR(500) DEFAULT NULL,
    reference_price DECIMAL(10, 2) DEFAULT NULL,
    submitted_at DATETIME DEFAULT NULL,
    approved_at DATETIME DEFAULT NULL,
    approved_by BIGINT DEFAULT NULL,
    rejection_reason VARCHAR(512) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_coach_application_coach_id ON coach_application (coach_id);
CREATE INDEX idx_coach_application_status ON coach_application (status);
CREATE INDEX idx_coach_application_coach_status_created ON coach_application (coach_id, status, created_at DESC);

-- 申请快照关联证书图片
CREATE TABLE IF NOT EXISTS coach_certificate_application (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    cert_type VARCHAR(32) NOT NULL,
    image_url VARCHAR(512) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_certificate_application_id ON coach_certificate_application (application_id);
CREATE INDEX idx_certificate_application_id_type ON coach_certificate_application (application_id, cert_type);

-- 已生效证书表（审核通过后写入）
CREATE TABLE IF NOT EXISTS coach_certificate (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coach_id BIGINT NOT NULL,
    cert_type VARCHAR(32) NOT NULL,
    image_url VARCHAR(512) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_coach_certificate_coach_id ON coach_certificate (coach_id);
CREATE INDEX idx_coach_certificate_coach_id_type ON coach_certificate (coach_id, cert_type);

-- 教练审核日志表
CREATE TABLE IF NOT EXISTS coach_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coach_id BIGINT NOT NULL,
    application_id BIGINT DEFAULT NULL,
    admin_id BIGINT DEFAULT NULL,
    action VARCHAR(16) NOT NULL,
    from_status SMALLINT NOT NULL,
    to_status SMALLINT NOT NULL,
    reason VARCHAR(512) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_coach_audit_log_coach_id ON coach_audit_log (coach_id);
CREATE INDEX idx_coach_audit_log_application_id ON coach_audit_log (application_id);
CREATE INDEX idx_coach_audit_log_created_at ON coach_audit_log (created_at);
