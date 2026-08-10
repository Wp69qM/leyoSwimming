ALTER TABLE `user`
    ADD COLUMN deleted_at DATETIME DEFAULT NULL COMMENT '注销时间' AFTER status,
    ADD COLUMN anonymous_after DATETIME DEFAULT NULL COMMENT '90 天后匿名化时间' AFTER deleted_at;
