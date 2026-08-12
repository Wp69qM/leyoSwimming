ALTER TABLE `coach_application`
    ADD COLUMN `phone_hash` VARCHAR(64) NULL DEFAULT NULL COMMENT '手机号哈希快照' AFTER `phone`;
