ALTER TABLE `package`
    ADD COLUMN `package_mode` VARCHAR(20) NOT NULL DEFAULT 'standard' COMMENT '套餐模式：standard / experience' AFTER `coach_id`,
    ADD COLUMN `original_price` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '购买时原价' AFTER `paid_amount`,
    ADD COLUMN `refund_enabled` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否可退款' AFTER `original_price`,
    ADD COLUMN `refund_ratio` DECIMAL(3,2) NOT NULL DEFAULT 1.00 COMMENT '退款比例' AFTER `refund_enabled`,
    ADD COLUMN `refund_valid_days` INT NOT NULL DEFAULT 0 COMMENT '退款有效天数' AFTER `refund_ratio`,
    ADD KEY idx_package_package_mode (`package_mode`);
