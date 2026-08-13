-- 扩展 admin_user 表：支持手机号（列表页展示）
ALTER TABLE `admin_user`
    ADD COLUMN `phone` VARCHAR(20) NULL DEFAULT NULL COMMENT '手机号' AFTER `name`;
