-- 修复退款比例字段精度不足，允许 0-100 的百分比值
ALTER TABLE `order`
    MODIFY COLUMN `refund_ratio` DECIMAL(5,2) DEFAULT NULL COMMENT '退款比例快照 0-100';

ALTER TABLE `package`
    MODIFY COLUMN `refund_ratio` DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '退款比例 0-100';
