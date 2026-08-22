ALTER TABLE `package` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `refunded_at`;
ALTER TABLE `package` ADD COLUMN `extend_reason` VARCHAR(200) DEFAULT NULL COMMENT '管理员手动延期原因' AFTER `expire_at`;
