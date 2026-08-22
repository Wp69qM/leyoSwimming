-- 为 order 表添加套餐快照字段与支付超时字段（测试环境使用 VARCHAR 替代 JSON，H2 需单列单条 ALTER）
ALTER TABLE `order` ADD COLUMN `expire_at` DATETIME DEFAULT NULL COMMENT '订单支付截止时间';
ALTER TABLE `order` ADD COLUMN `package_mode` VARCHAR(20) DEFAULT NULL COMMENT '套餐模式：standard / experience';
ALTER TABLE `order` ADD COLUMN `package_template_id` BIGINT DEFAULT NULL COMMENT '套餐模板 ID';
ALTER TABLE `order` ADD COLUMN `package_name` VARCHAR(128) DEFAULT NULL COMMENT '套餐名称快照';
ALTER TABLE `order` ADD COLUMN `coach_name` VARCHAR(64) DEFAULT NULL COMMENT '教练姓名快照';
ALTER TABLE `order` ADD COLUMN `teaching_type` VARCHAR(20) DEFAULT NULL COMMENT '班级规模快照';
ALTER TABLE `order` ADD COLUMN `stroke_ids` VARCHAR(2000) DEFAULT NULL COMMENT '泳姿 ID 列表快照';
ALTER TABLE `order` ADD COLUMN `total_hours` INT DEFAULT NULL COMMENT '总课时快照';
ALTER TABLE `order` ADD COLUMN `duration_minutes` INT DEFAULT NULL COMMENT '每节课时长快照（分钟）';
ALTER TABLE `order` ADD COLUMN `valid_days` INT DEFAULT NULL COMMENT '有效期天数快照';
ALTER TABLE `order` ADD COLUMN `refund_enabled` TINYINT(1) DEFAULT NULL COMMENT '是否可退款快照';
ALTER TABLE `order` ADD COLUMN `refund_ratio` DECIMAL(3,2) DEFAULT NULL COMMENT '退款比例快照';
ALTER TABLE `order` ADD COLUMN `refund_valid_days` INT DEFAULT NULL COMMENT '退款有效期天数快照';
ALTER TABLE `order` ADD COLUMN `paid_at` DATETIME DEFAULT NULL COMMENT '支付成功时间';
CREATE INDEX idx_order_expire_at ON `order` (expire_at);
CREATE INDEX idx_order_package_template_id ON `order` (package_template_id);

-- 为 package 表添加关联订单与快照字段
ALTER TABLE `package` ADD COLUMN `order_id` BIGINT DEFAULT NULL COMMENT '关联购买订单 ID';
ALTER TABLE `package` ADD COLUMN `package_name` VARCHAR(128) DEFAULT NULL COMMENT '套餐名称快照';
ALTER TABLE `package` ADD COLUMN `coach_name` VARCHAR(64) DEFAULT NULL COMMENT '教练姓名快照';
ALTER TABLE `package` ADD COLUMN `teaching_type` VARCHAR(20) DEFAULT NULL COMMENT '班级规模快照';
ALTER TABLE `package` ADD COLUMN `stroke_ids` VARCHAR(2000) DEFAULT NULL COMMENT '泳姿 ID 列表快照';
ALTER TABLE `package` ADD COLUMN `duration_minutes` INT DEFAULT NULL COMMENT '每节课时长快照（分钟）';
ALTER TABLE `package` ADD COLUMN `valid_days` INT DEFAULT NULL COMMENT '有效期天数快照';
CREATE INDEX idx_package_order_id ON `package` (order_id);
