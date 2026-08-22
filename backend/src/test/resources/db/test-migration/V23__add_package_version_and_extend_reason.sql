ALTER TABLE `package` ADD COLUMN `version` INT NOT NULL DEFAULT 0;
ALTER TABLE `package` ADD COLUMN `extend_reason` VARCHAR(200) DEFAULT NULL;

COMMENT ON COLUMN `package`.`version` IS '乐观锁版本号';
COMMENT ON COLUMN `package`.`extend_reason` IS '管理员手动延期原因';
