-- 为 package 表添加套餐编号字段（测试环境）
ALTER TABLE `package` ADD COLUMN `package_no` VARCHAR(32) DEFAULT NULL COMMENT '套餐编号';
CREATE UNIQUE INDEX `uk_package_no` ON `package` (`package_no`);

-- H2 不支持 DATE_FORMAT / LPAD / RAND 组合生成唯一编号，测试数据由应用层生成
