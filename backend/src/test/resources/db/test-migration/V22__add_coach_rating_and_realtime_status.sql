-- US-001 教练列表/详情：补充公开展示所需字段（H2 兼容语法，COMMENT 在 H2 ADD COLUMN 中不支持）
ALTER TABLE `coach` ADD COLUMN `rating` DECIMAL(2,1) NOT NULL DEFAULT 5.0;
ALTER TABLE `coach` ADD COLUMN `realtime_status` VARCHAR(16) NOT NULL DEFAULT '空闲中';

COMMENT ON COLUMN `coach`.`rating` IS '综合评分 0.0-5.0';
COMMENT ON COLUMN `coach`.`realtime_status` IS '实时状态：空闲中/上课中/休息中/已下班/请假中';

-- 列表页核心查询：公开状态 + 评分排序
CREATE INDEX IF NOT EXISTS `idx_coach_status_rating` ON `coach` (`status`, `rating` DESC);
