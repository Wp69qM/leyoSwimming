-- 教练资料扩展字段
ALTER TABLE `coach`
    ADD COLUMN `age` INT DEFAULT NULL COMMENT '年龄 3-99',
    ADD COLUMN `gender` VARCHAR(10) DEFAULT NULL COMMENT '性别: male/female',
    ADD COLUMN `profile_completed` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '资料是否已完善: 0-否,1-是',
    ADD COLUMN `personal_desc` VARCHAR(512) DEFAULT NULL COMMENT '个人简介',
    ADD COLUMN `teaching_years` INT DEFAULT NULL COMMENT '执教年限',
    ADD COLUMN `certificates` JSON DEFAULT NULL COMMENT '资质证书列表，如 [{"name":"救生员证","url":"..."}]';
