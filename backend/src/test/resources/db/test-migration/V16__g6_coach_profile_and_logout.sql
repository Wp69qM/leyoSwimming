-- G6 个人主页与退出登录相关表结构变更（H2 兼容版）

-- 1. 扩展 coach 表：支持参考单价变更频率限制
ALTER TABLE `coach` ADD COLUMN `price_changed_at` DATETIME DEFAULT NULL;
ALTER TABLE `coach` ADD COLUMN `price_change_count_today` INT NOT NULL DEFAULT 0;

-- 2. 教练主页变更日志表（US-012）
CREATE TABLE IF NOT EXISTS `coach_update_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `coach_id` BIGINT NOT NULL,
    `field_name` VARCHAR(32) NOT NULL,
    `old_value` TEXT,
    `new_value` TEXT,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX `idx_coach_update_log_coach_id` ON `coach_update_log` (`coach_id`);
CREATE INDEX `idx_coach_update_log_created_at` ON `coach_update_log` (`created_at`);

-- 3. coach_certificate 复合索引（US-012 读取 PORTRAIT 与资质图片）
CREATE INDEX IF NOT EXISTS `idx_coach_certificate_coach_id_type` ON `coach_certificate` (`coach_id`, `cert_type`);
