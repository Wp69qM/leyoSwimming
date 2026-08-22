-- 为 order 表添加套餐快照字段与支付超时字段
ALTER TABLE `order`
    ADD COLUMN `expire_at` DATETIME DEFAULT NULL COMMENT '订单支付截止时间' AFTER `refunded_at`,
    ADD COLUMN `package_mode` VARCHAR(20) DEFAULT NULL COMMENT '套餐模式：standard / experience' AFTER `expire_at`,
    ADD COLUMN `package_template_id` BIGINT DEFAULT NULL COMMENT '套餐模板 ID' AFTER `package_mode`,
    ADD COLUMN `package_name` VARCHAR(128) DEFAULT NULL COMMENT '套餐名称快照' AFTER `package_template_id`,
    ADD COLUMN `coach_name` VARCHAR(64) DEFAULT NULL COMMENT '教练姓名快照' AFTER `package_name`,
    ADD COLUMN `teaching_type` VARCHAR(20) DEFAULT NULL COMMENT '班级规模快照' AFTER `coach_name`,
    ADD COLUMN `stroke_ids` JSON DEFAULT NULL COMMENT '泳姿 ID 列表快照' AFTER `teaching_type`,
    ADD COLUMN `total_hours` INT DEFAULT NULL COMMENT '总课时快照' AFTER `stroke_ids`,
    ADD COLUMN `duration_minutes` INT DEFAULT NULL COMMENT '每节课时长快照（分钟）' AFTER `total_hours`,
    ADD COLUMN `valid_days` INT DEFAULT NULL COMMENT '有效期天数快照' AFTER `duration_minutes`,
    ADD COLUMN `refund_enabled` TINYINT(1) DEFAULT NULL COMMENT '是否可退款快照' AFTER `valid_days`,
    ADD COLUMN `refund_ratio` DECIMAL(3,2) DEFAULT NULL COMMENT '退款比例快照' AFTER `refund_enabled`,
    ADD COLUMN `refund_valid_days` INT DEFAULT NULL COMMENT '退款有效期天数快照' AFTER `refund_ratio`,
    ADD COLUMN `paid_at` DATETIME DEFAULT NULL COMMENT '支付成功时间' AFTER `refund_valid_days`,
    ADD KEY idx_order_expire_at (expire_at),
    ADD KEY idx_order_package_template_id (package_template_id);

-- 为 package 表添加关联订单与快照字段
ALTER TABLE `package`
    ADD COLUMN `order_id` BIGINT DEFAULT NULL COMMENT '关联购买订单 ID' AFTER `coach_id`,
    ADD COLUMN `package_name` VARCHAR(128) DEFAULT NULL COMMENT '套餐名称快照' AFTER `refund_valid_days`,
    ADD COLUMN `coach_name` VARCHAR(64) DEFAULT NULL COMMENT '教练姓名快照' AFTER `package_name`,
    ADD COLUMN `teaching_type` VARCHAR(20) DEFAULT NULL COMMENT '班级规模快照' AFTER `coach_name`,
    ADD COLUMN `stroke_ids` JSON DEFAULT NULL COMMENT '泳姿 ID 列表快照' AFTER `teaching_type`,
    ADD COLUMN `duration_minutes` INT DEFAULT NULL COMMENT '每节课时长快照（分钟）' AFTER `stroke_ids`,
    ADD COLUMN `valid_days` INT DEFAULT NULL COMMENT '有效期天数快照' AFTER `duration_minutes`,
    ADD KEY idx_package_order_id (order_id);
