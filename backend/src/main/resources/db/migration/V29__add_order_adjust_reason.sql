-- 为 order 表添加退款审批调整原因字段
ALTER TABLE `order`
    ADD COLUMN `adjust_reason` VARCHAR(500) DEFAULT NULL COMMENT '退款金额调整原因' AFTER `rejected_reason`;
