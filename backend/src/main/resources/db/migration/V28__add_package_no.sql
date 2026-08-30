-- 为 package 表添加套餐编号字段
ALTER TABLE `package`
    ADD COLUMN `package_no` VARCHAR(32) DEFAULT NULL COMMENT '套餐编号' AFTER `id`,
    ADD UNIQUE KEY `uk_package_no` (`package_no`);

-- 为已有套餐生成编号（格式：P-YYYYMMDDHHMMSS-XXXX）
UPDATE `package` SET `package_no` = CONCAT('P-', DATE_FORMAT(`created_at`, '%Y%m%d%H%i%s'), LPAD(FLOOR(RAND() * 10000), 4, '0')) WHERE `package_no` IS NULL;
