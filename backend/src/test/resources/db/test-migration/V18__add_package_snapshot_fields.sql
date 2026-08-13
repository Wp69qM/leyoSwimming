ALTER TABLE `package` ADD COLUMN `package_mode` VARCHAR(20) NOT NULL DEFAULT 'standard';
ALTER TABLE `package` ADD COLUMN `original_price` DECIMAL(10,2) NOT NULL DEFAULT 0.00;
ALTER TABLE `package` ADD COLUMN `refund_enabled` TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE `package` ADD COLUMN `refund_ratio` DECIMAL(3,2) NOT NULL DEFAULT 1.00;
ALTER TABLE `package` ADD COLUMN `refund_valid_days` INT NOT NULL DEFAULT 0;

CREATE INDEX idx_package_package_mode ON `package` (`package_mode`);
