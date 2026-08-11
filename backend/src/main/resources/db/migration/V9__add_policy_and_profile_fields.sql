-- 用户资料扩展字段
ALTER TABLE `user`
    ADD COLUMN `age` INT DEFAULT NULL COMMENT '年龄 3-99',
    ADD COLUMN `gender` VARCHAR(10) DEFAULT NULL COMMENT '性别: male/female',
    ADD COLUMN `guardian_name` VARCHAR(64) DEFAULT NULL COMMENT '监护人姓名（未成年人）',
    ADD COLUMN `guardian_phone` VARCHAR(128) DEFAULT NULL COMMENT '监护人手机号（AES 加密）',
    ADD COLUMN `has_swim_basis` TINYINT(1) DEFAULT NULL COMMENT '是否有游泳基础: 0-否,1-是',
    ADD COLUMN `swim_strokes` JSON DEFAULT NULL COMMENT '会什么泳姿，如 ["breaststroke","freestyle"]',
    ADD COLUMN `swim_years` INT DEFAULT NULL COMMENT '游泳年限',
    ADD COLUMN `personal_desc` VARCHAR(512) DEFAULT NULL COMMENT '个人描述';

-- 用户须知版本表
CREATE TABLE IF NOT EXISTS `terms_policy` (
    `version` VARCHAR(32) NOT NULL PRIMARY KEY COMMENT '版本号，如 v1.0',
    `content` TEXT NOT NULL COMMENT '协议正文（Markdown / 富文本）',
    `effective_at` DATETIME NOT NULL COMMENT '生效时间',
    `is_current` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1=当前生效，同一时刻仅 1 条可为 1',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY `idx_terms_policy_current` (`is_current`, `effective_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户须知版本表';

-- 隐私协议版本表
CREATE TABLE IF NOT EXISTS `privacy_policy` (
    `version` VARCHAR(32) NOT NULL PRIMARY KEY COMMENT '版本号，如 v1.0',
    `content` TEXT NOT NULL COMMENT '协议正文（Markdown / 富文本）',
    `effective_at` DATETIME NOT NULL COMMENT '生效时间',
    `is_current` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1=当前生效，同一时刻仅 1 条可为 1',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY `idx_privacy_policy_current` (`is_current`, `effective_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='隐私协议版本表';

-- 用户/教练用户须知同意记录表
CREATE TABLE IF NOT EXISTS `user_terms_consent` (
    `consent_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `actor_type` VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT 'user 或 coach',
    `user_id` BIGINT NOT NULL COMMENT '用户/教练 ID',
    `version` VARCHAR(32) NOT NULL COMMENT '协议版本',
    `status` VARCHAR(20) NOT NULL DEFAULT 'agreed' COMMENT '同意状态，本 US 仅 agreed',
    `agreed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_actor_user_terms_version` (`actor_type`, `user_id`, `version`),
    KEY `idx_user_terms_user_id` (`actor_type`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户/教练用户须知同意记录';

-- 用户/教练隐私协议同意记录表
CREATE TABLE IF NOT EXISTS `user_privacy_consent` (
    `consent_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `actor_type` VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT 'user 或 coach',
    `user_id` BIGINT NOT NULL COMMENT '用户/教练 ID',
    `version` VARCHAR(32) NOT NULL COMMENT '协议版本',
    `status` VARCHAR(20) NOT NULL DEFAULT 'agreed' COMMENT '同意状态，本 US 仅 agreed',
    `agreed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_actor_user_privacy_version` (`actor_type`, `user_id`, `version`),
    KEY `idx_user_privacy_user_id` (`actor_type`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户/教练隐私协议同意记录';

-- 默认生效版本（开发期占位内容）
INSERT INTO `terms_policy` (`version`, `content`, `effective_at`, `is_current`)
VALUES ('v1.0', '# 用户须知\n\n欢迎使用 leyoSwimming。\n\n1. 请遵守场馆规则。\n2. 未成年人需在监护人陪同下使用相关服务。', NOW(), 1)
ON DUPLICATE KEY UPDATE `is_current` = 1;

INSERT INTO `privacy_policy` (`version`, `content`, `effective_at`, `is_current`)
VALUES ('v1.0', '# 隐私协议\n\n我们重视您的隐私。\n\n1. 我们仅收集提供服务所必需的信息。\n2. 您的手机号等敏感信息将加密存储。', NOW(), 1)
ON DUPLICATE KEY UPDATE `is_current` = 1;
