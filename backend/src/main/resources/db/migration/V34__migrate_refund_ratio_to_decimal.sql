-- 将历史退款比例从百分比（80 表示 80%）转换为小数（0.8 表示 80%）
-- package_template 中的值均来自管理端百分比输入，统一除以 100
UPDATE `package_template`
SET `refund_ratio` = `refund_ratio` / 100
WHERE `refund_ratio` IS NOT NULL AND `refund_ratio` <> 0;

-- package / order 中的快照值：大于 1 的视为历史百分比数据，转换为小数；0 与 1 保持原值
UPDATE `package`
SET `refund_ratio` = `refund_ratio` / 100
WHERE `refund_ratio` IS NOT NULL AND `refund_ratio` > 1;

UPDATE `order`
SET `refund_ratio` = `refund_ratio` / 100
WHERE `refund_ratio` IS NOT NULL AND `refund_ratio` > 1;
