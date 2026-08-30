-- G6 个人主页与退出登录相关表结构变更

-- 1. 扩展 coach 表：支持参考单价变更频率限制
SET @dbname = DATABASE();

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = @dbname AND table_name = 'coach' AND column_name = 'price_changed_at') = 0,
    'ALTER TABLE `coach` ADD COLUMN `price_changed_at` DATETIME DEFAULT NULL COMMENT "上次改价时间"',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = @dbname AND table_name = 'coach' AND column_name = 'price_change_count_today') = 0,
    'ALTER TABLE `coach` ADD COLUMN `price_change_count_today` INT NOT NULL DEFAULT 0 COMMENT "今日改价次数"',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 教练主页变更日志表（US-012）
CREATE TABLE IF NOT EXISTS `coach_update_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `coach_id` BIGINT NOT NULL COMMENT '教练 ID',
    `field_name` VARCHAR(32) NOT NULL COMMENT '变更字段，如 reference_price/bio',
    `old_value` TEXT COMMENT '变更前值',
    `new_value` TEXT COMMENT '变更后值',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_coach_update_log_coach_id` (`coach_id`),
    KEY `idx_coach_update_log_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教练主页变更日志表';

-- 3. 确认 coach_certificate 表复合索引（US-012 读取 PORTRAIT 与资质图片）
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.statistics
     WHERE table_schema = @dbname AND table_name = 'coach_certificate' AND index_name = 'idx_coach_certificate_coach_id_type') = 0,
    'CREATE INDEX `idx_coach_certificate_coach_id_type` ON `coach_certificate` (`coach_id`, `cert_type`)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
