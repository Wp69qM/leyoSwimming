-- 教练端学员信息切片表
CREATE TABLE IF NOT EXISTS `coach_student_profile`
(
    `profile_id`       BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    `coach_id`         BIGINT      NOT NULL COMMENT '教练 ID',
    `student_user_id`  BIGINT      NOT NULL COMMENT '学员用户 ID',
    `learning_strokes` VARCHAR(64) DEFAULT NULL COMMENT '学习泳姿，如"自由泳/蛙泳"',
    `swim_level`       TINYINT     DEFAULT NULL COMMENT '游泳等级：0=零基础 1=入门 2=进阶 3=高级',
    `basics`           VARCHAR(500) DEFAULT NULL COMMENT '基础情况描述',
    `notes`            VARCHAR(1000) DEFAULT NULL COMMENT '沟通备注',
    `created_at`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `idx_coach_student` (`coach_id`, `student_user_id`),
    KEY `idx_coach_id` (`coach_id`),
    KEY `idx_student_user_id` (`student_user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='教练端学员信息切片';
