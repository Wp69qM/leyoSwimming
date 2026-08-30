ALTER TABLE `user` ADD COLUMN `age` INT DEFAULT NULL COMMENT '年龄 3-99';
ALTER TABLE `user` ADD COLUMN `gender` VARCHAR(10) DEFAULT NULL COMMENT '性别: male/female';
ALTER TABLE `user` ADD COLUMN `guardian_name` VARCHAR(64) DEFAULT NULL COMMENT '监护人姓名（未成年人）';
ALTER TABLE `user` ADD COLUMN `guardian_phone` VARCHAR(128) DEFAULT NULL COMMENT '监护人手机号（AES 加密）';
ALTER TABLE `user` ADD COLUMN `has_swim_basis` TINYINT(1) DEFAULT NULL COMMENT '是否有游泳基础: 0-否,1-是';
ALTER TABLE `user` ADD COLUMN `swim_strokes` JSON DEFAULT NULL COMMENT '会什么泳姿';
ALTER TABLE `user` ADD COLUMN `swim_years` INT DEFAULT NULL COMMENT '游泳年限';
ALTER TABLE `user` ADD COLUMN `personal_desc` VARCHAR(512) DEFAULT NULL COMMENT '个人描述';

CREATE TABLE IF NOT EXISTS `terms_policy` (
    `version` VARCHAR(32) NOT NULL PRIMARY KEY,
    `content` TEXT NOT NULL,
    `effective_at` DATETIME NOT NULL,
    `is_current` TINYINT(1) NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `privacy_policy` (
    `version` VARCHAR(32) NOT NULL PRIMARY KEY,
    `content` TEXT NOT NULL,
    `effective_at` DATETIME NOT NULL,
    `is_current` TINYINT(1) NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `user_terms_consent` (
    `consent_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `actor_type` VARCHAR(20) NOT NULL DEFAULT 'user',
    `user_id` BIGINT NOT NULL,
    `version` VARCHAR(32) NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'agreed',
    `agreed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_actor_user_terms_version` (`actor_type`, `user_id`, `version`),
    KEY `idx_user_terms_user_id` (`actor_type`, `user_id`)
);

CREATE TABLE IF NOT EXISTS `user_privacy_consent` (
    `consent_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `actor_type` VARCHAR(20) NOT NULL DEFAULT 'user',
    `user_id` BIGINT NOT NULL,
    `version` VARCHAR(32) NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'agreed',
    `agreed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_actor_user_privacy_version` (`actor_type`, `user_id`, `version`),
    KEY `idx_user_privacy_user_id` (`actor_type`, `user_id`)
);

INSERT INTO `terms_policy` (`version`, `content`, `effective_at`, `is_current`)
VALUES ('v1.0', '# 用户须知\n\n欢迎使用 leyoSwimming。', NOW(), 1);

INSERT INTO `privacy_policy` (`version`, `content`, `effective_at`, `is_current`)
VALUES ('v1.0', '# 隐私协议\n\n我们重视您的隐私。', NOW(), 1);
