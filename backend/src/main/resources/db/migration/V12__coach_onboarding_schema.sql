-- G3 教练入驻/审核/重新入驻相关表结构

-- 扩展 coach 表字段（保留现有 certificates JSON 字段以兼容 G2 资料页）
ALTER TABLE `coach`
    ADD COLUMN `email` VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    ADD COLUMN `wechat_qr_url` VARCHAR(512) DEFAULT NULL COMMENT '微信二维码图片 URL',
    ADD COLUMN `id_card_no` VARCHAR(128) DEFAULT NULL COMMENT 'AES 加密身份证号',
    ADD COLUMN `total_students` INT DEFAULT NULL COMMENT '总学员数 0-99999',
    ADD COLUMN `total_hours` INT DEFAULT NULL COMMENT '总课时数 0-99999',
    ADD COLUMN `teaching_strokes` VARCHAR(128) DEFAULT NULL COMMENT '擅长泳姿，逗号分隔',
    ADD COLUMN `bio` VARCHAR(500) DEFAULT NULL COMMENT '个人简介 10-500 字符',
    ADD COLUMN `reference_price` DECIMAL(10, 2) DEFAULT NULL COMMENT '参考单价（元/节）50-2000',
    ADD COLUMN `approved_at` DATETIME DEFAULT NULL COMMENT '最近一次审核通过时间';

-- 教练入驻申请快照表
CREATE TABLE IF NOT EXISTS coach_application (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coach_id BIGINT NOT NULL COMMENT '教练 ID',
    status VARCHAR(16) NOT NULL COMMENT 'draft/pending/approved/rejected',
    previous_coach_status SMALLINT DEFAULT NULL COMMENT '提交前 coach.status：-1/2/3',
    name VARCHAR(64) DEFAULT NULL COMMENT '姓名/昵称快照',
    phone VARCHAR(128) DEFAULT NULL COMMENT '手机号快照（AES 加密）',
    gender VARCHAR(10) DEFAULT NULL COMMENT '性别快照: male/female',
    age INT DEFAULT NULL COMMENT '年龄快照',
    email VARCHAR(128) DEFAULT NULL COMMENT '邮箱快照',
    wechat_qr_url VARCHAR(512) DEFAULT NULL COMMENT '微信二维码快照',
    id_card_no VARCHAR(128) DEFAULT NULL COMMENT '身份证号快照（AES 加密）',
    teaching_years INT DEFAULT NULL COMMENT '任教年限快照',
    total_students INT DEFAULT NULL COMMENT '总学员数快照',
    total_hours INT DEFAULT NULL COMMENT '总课时数快照',
    teaching_strokes VARCHAR(128) DEFAULT NULL COMMENT '擅长泳姿快照',
    bio VARCHAR(500) DEFAULT NULL COMMENT '个人简介快照',
    reference_price DECIMAL(10, 2) DEFAULT NULL COMMENT '参考单价快照',
    submitted_at DATETIME DEFAULT NULL COMMENT '正式提交时间；draft 时为 NULL',
    approved_at DATETIME DEFAULT NULL COMMENT '审核通过时间',
    approved_by BIGINT DEFAULT NULL COMMENT '审核管理员 ID',
    rejection_reason VARCHAR(512) DEFAULT NULL COMMENT '驳回原因',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_coach_application_coach_id (coach_id),
    KEY idx_coach_application_status (status),
    KEY idx_coach_application_coach_status_created (coach_id, status, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教练入驻申请快照表';

-- 申请快照关联证书图片
CREATE TABLE IF NOT EXISTS coach_certificate_application (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL COMMENT '申请快照 ID',
    cert_type VARCHAR(32) NOT NULL COMMENT 'ID_CARD_FRONT/ID_CARD_BACK/COACH_CERT/HEALTH_CERT/PORTRAIT/OTHER',
    image_url VARCHAR(512) NOT NULL COMMENT '图片 URL',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '同类型证书排序',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_certificate_application_id (application_id),
    KEY idx_certificate_application_id_type (application_id, cert_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='申请快照证书表';

-- 已生效证书表（审核通过后写入）
CREATE TABLE IF NOT EXISTS coach_certificate (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coach_id BIGINT NOT NULL COMMENT '教练 ID',
    cert_type VARCHAR(32) NOT NULL COMMENT '证书类型',
    image_url VARCHAR(512) NOT NULL COMMENT '图片 URL',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_coach_certificate_coach_id (coach_id),
    KEY idx_coach_certificate_coach_id_type (coach_id, cert_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教练生效证书表';

-- 教练审核日志表
CREATE TABLE IF NOT EXISTS coach_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coach_id BIGINT NOT NULL COMMENT '教练 ID',
    application_id BIGINT DEFAULT NULL COMMENT '关联申请快照 ID；draft_save 可为 NULL',
    admin_id BIGINT DEFAULT NULL COMMENT '管理员 ID；教练自身动作为 NULL',
    action VARCHAR(16) NOT NULL COMMENT 'submit/approve/reject/draft_save',
    from_status SMALLINT NOT NULL COMMENT '变更前 coach.status',
    to_status SMALLINT NOT NULL COMMENT '变更后 coach.status',
    reason VARCHAR(512) DEFAULT NULL COMMENT '原因',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_coach_audit_log_coach_id (coach_id),
    KEY idx_coach_audit_log_application_id (application_id),
    KEY idx_coach_audit_log_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教练审核日志表';
