-- G5 管理员账号/用户账号/教练账号管理相关表结构变更

-- 1. 扩展 user 表：支持身份、来源、乐观锁；管理员新建用户可能没有 openid
ALTER TABLE `user`
    ADD COLUMN `identity` TINYINT NOT NULL DEFAULT 1 COMMENT '身份：0-游客，1-注册用户，2-学员',
    ADD COLUMN `source` VARCHAR(32) NOT NULL DEFAULT 'WECHAT' COMMENT '来源：WECHAT/PHONE/ADMIN_CREATED',
    ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    MODIFY COLUMN `openid` VARCHAR(64) NULL COMMENT '微信 openid（管理员新建时可为空）';

-- 将现有 identity_status 映射到 identity（简化处理，已完善资料视为注册用户）
UPDATE `user` SET `identity` = 1 WHERE `identity_status` = '注册用户';
UPDATE `user` SET `identity` = 0 WHERE `identity_status` = '游客';

-- 2. 扩展 coach 表：支持身份证哈希、乐观锁；管理员新建教练可能没有 openid
ALTER TABLE `coach`
    MODIFY COLUMN `openid` VARCHAR(64) NULL COMMENT '微信 openid（管理员新建时可为空）',
    ADD COLUMN `id_card_hash` VARCHAR(64) NULL DEFAULT NULL COMMENT '身份证号哈希',
    ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号';

-- 3. 教练手机号唯一索引（排除已离职，允许复用手机号）
ALTER TABLE `coach` ADD UNIQUE INDEX `uk_coach_phone_hash` (`phone_hash`);

-- 4. 教练身份证号唯一索引（已离职教练也保留历史身份证，不强制复用）
ALTER TABLE `coach` ADD UNIQUE INDEX `uk_coach_id_card_hash` (`id_card_hash`);

-- 5. 扩展 admin_user 表：支持逻辑删除
ALTER TABLE `admin_user`
    ADD COLUMN `deleted_at` DATETIME DEFAULT NULL COMMENT '逻辑删除时间',
    ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号';

-- 6. 用户操作审计日志表（US-042 管理员管理用户账号）
CREATE TABLE IF NOT EXISTS `audit_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `actor_type` VARCHAR(20) NOT NULL DEFAULT 'admin' COMMENT '操作者类型：admin',
    `actor_id` BIGINT NOT NULL COMMENT '操作者 ID',
    `target_type` VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '目标类型：user/coach/admin',
    `target_id` BIGINT NOT NULL COMMENT '目标 ID',
    `action` VARCHAR(32) NOT NULL COMMENT '操作类型：ADMIN_CREATE_USER/ADMIN_UPDATE_PROFILE/ADMIN_BAN_USER/ADMIN_UNBAN_USER 等',
    `before_snapshot` JSON DEFAULT NULL COMMENT '变更前快照',
    `after_snapshot` JSON DEFAULT NULL COMMENT '变更后快照',
    `reason` VARCHAR(512) DEFAULT NULL COMMENT '操作原因',
    `ip` VARCHAR(64) DEFAULT NULL COMMENT '操作 IP',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_audit_log_target` (`target_type`, `target_id`),
    KEY `idx_audit_log_actor` (`actor_type`, `actor_id`),
    KEY `idx_audit_log_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作审计日志表';

-- 7. 管理员账号操作审计日志表（US-057 管理员管理管理员账号）
CREATE TABLE IF NOT EXISTS `admin_audit_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `admin_user_id` BIGINT NOT NULL COMMENT '操作人 ID',
    `target_admin_user_id` BIGINT NOT NULL COMMENT '目标管理员账号 ID',
    `action` VARCHAR(32) NOT NULL COMMENT '操作类型：CREATE/UPDATE/DISABLE/ENABLE/DELETE/RESET_PASSWORD',
    `before_snapshot` JSON DEFAULT NULL COMMENT '变更前快照',
    `after_snapshot` JSON DEFAULT NULL COMMENT '变更后快照',
    `reason` VARCHAR(512) DEFAULT NULL COMMENT '操作原因',
    `ip` VARCHAR(64) DEFAULT NULL COMMENT '操作 IP',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_admin_audit_log_target` (`target_admin_user_id`),
    KEY `idx_admin_audit_log_actor` (`admin_user_id`),
    KEY `idx_admin_audit_log_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员账号审计日志表';
