-- G5 管理员账号/用户账号/教练账号管理相关表结构变更

-- 1. 扩展 user 表：支持身份、来源、乐观锁；管理员新建用户可能没有 openid
ALTER TABLE `user` ADD COLUMN `identity` TINYINT NOT NULL DEFAULT 1;
ALTER TABLE `user` ADD COLUMN `source` VARCHAR(32) NOT NULL DEFAULT 'WECHAT';
ALTER TABLE `user` ADD COLUMN `version` INT NOT NULL DEFAULT 0;
ALTER TABLE `user` MODIFY COLUMN `openid` VARCHAR(64) NULL;

-- 将现有 identity_status 映射到 identity
UPDATE `user` SET `identity` = 1 WHERE `identity_status` = '注册用户';
UPDATE `user` SET `identity` = 0 WHERE `identity_status` = '游客';

-- 2. 扩展 coach 表：支持身份证哈希、乐观锁
ALTER TABLE `coach` MODIFY COLUMN `openid` VARCHAR(64) NULL;
ALTER TABLE `coach` ADD COLUMN `id_card_hash` VARCHAR(64) NULL DEFAULT NULL;
ALTER TABLE `coach` ADD COLUMN `version` INT NOT NULL DEFAULT 0;

-- 3. 教练手机号唯一索引（排除已离职，允许复用手机号）
ALTER TABLE `coach` ADD UNIQUE INDEX `uk_coach_phone_hash` (`phone_hash`);

-- 4. 教练身份证号唯一索引
ALTER TABLE `coach` ADD UNIQUE INDEX `uk_coach_id_card_hash` (`id_card_hash`);

-- 5. 扩展 admin_user 表：支持逻辑删除
ALTER TABLE `admin_user` ADD COLUMN `deleted_at` DATETIME DEFAULT NULL;
ALTER TABLE `admin_user` ADD COLUMN `version` INT NOT NULL DEFAULT 0;

-- 6. 用户操作审计日志表
CREATE TABLE IF NOT EXISTS `audit_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `actor_type` VARCHAR(20) NOT NULL DEFAULT 'admin',
    `actor_id` BIGINT NOT NULL,
    `target_type` VARCHAR(20) NOT NULL DEFAULT 'user',
    `target_id` BIGINT NOT NULL,
    `action` VARCHAR(32) NOT NULL,
    `before_snapshot` JSON DEFAULT NULL,
    `after_snapshot` JSON DEFAULT NULL,
    `reason` VARCHAR(512) DEFAULT NULL,
    `ip` VARCHAR(64) DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX `idx_audit_log_target` ON `audit_log` (`target_type`, `target_id`);
CREATE INDEX `idx_audit_log_actor` ON `audit_log` (`actor_type`, `actor_id`);
CREATE INDEX `idx_audit_log_created_at` ON `audit_log` (`created_at`);

-- 7. 管理员账号操作审计日志表
CREATE TABLE IF NOT EXISTS `admin_audit_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `admin_user_id` BIGINT NOT NULL,
    `target_admin_user_id` BIGINT NOT NULL,
    `action` VARCHAR(32) NOT NULL,
    `before_snapshot` JSON DEFAULT NULL,
    `after_snapshot` JSON DEFAULT NULL,
    `reason` VARCHAR(512) DEFAULT NULL,
    `ip` VARCHAR(64) DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX `idx_admin_audit_log_target` ON `admin_audit_log` (`target_admin_user_id`);
CREATE INDEX `idx_admin_audit_log_actor` ON `admin_audit_log` (`admin_user_id`);
CREATE INDEX `idx_admin_audit_log_created_at` ON `admin_audit_log` (`created_at`);
