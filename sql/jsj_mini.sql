-- ========================================
-- 用户表
-- ========================================
CREATE TABLE IF NOT EXISTS `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` varchar(64) NOT NULL COMMENT '用户ID（登录用，业务标识）',
  `nickname` varchar(64) DEFAULT NULL COMMENT '昵称',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志：0=未删除 1=已删除',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ========================================
-- 用户敏感信息表
-- ========================================
CREATE TABLE IF NOT EXISTS `user_sensitive_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID（关联 user.id）',
  `open_id` varchar(128) DEFAULT NULL COMMENT '微信openId',
  `union_id` varchar(128) DEFAULT NULL COMMENT '微信unionId',
  `mini_app_id` varchar(64) DEFAULT NULL COMMENT '小程序appId',
  `official_app_id` varchar(64) DEFAULT NULL COMMENT '公众号appId',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志：0=未删除 1=已删除',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_open_id` (`open_id`),
  KEY `idx_union_id` (`union_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户敏感信息表';
