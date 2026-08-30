-- US-001 教练列表/详情：补充公开展示所需字段
SET @dbname = DATABASE();

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = @dbname AND table_name = 'coach' AND column_name = 'rating') = 0,
    'ALTER TABLE `coach` ADD COLUMN `rating` DECIMAL(2,1) NOT NULL DEFAULT 5.0 COMMENT "综合评分 0.0-5.0"',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = @dbname AND table_name = 'coach' AND column_name = 'realtime_status') = 0,
    'ALTER TABLE `coach` ADD COLUMN `realtime_status` VARCHAR(16) NOT NULL DEFAULT "空闲中" COMMENT "实时状态：空闲中/上课中/休息中/已下班/请假中"',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 列表页核心查询：公开状态 + 评分排序
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.statistics
     WHERE table_schema = @dbname AND table_name = 'coach' AND index_name = 'idx_coach_status_rating') = 0,
    'CREATE INDEX `idx_coach_status_rating` ON `coach` (`status`, `rating` DESC)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
