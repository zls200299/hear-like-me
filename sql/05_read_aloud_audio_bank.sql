-- ============================================================================
-- 点读音频库（增量）
-- Database: hear_like_me
-- 说明：可在已有库上重复执行前先判断；菜单增量见文末（hear_like_me_admin）
-- ============================================================================

SET NAMES utf8mb4;
USE `hear_like_me`;

CREATE TABLE IF NOT EXISTS `read_aloud_audio_bank` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `audio_code` varchar(64) NOT NULL COMMENT '音频唯一编码',
  `title` varchar(150) NOT NULL COMMENT '音频名称',
  `description` varchar(500) DEFAULT NULL COMMENT '素材说明',
  `source_asset_id` bigint NOT NULL COMMENT '原始音频 file_asset.id',
  `output_asset_id` bigint DEFAULT NULL COMMENT '生成后的模拟音频 file_asset.id',
  `processing_task_no` varchar(64) DEFAULT NULL COMMENT '最近一次处理任务编号',
  `n_channels` tinyint NOT NULL DEFAULT 8 COMMENT '有效通道数（制作参数，非答题答案）',
  `carrier` varchar(20) NOT NULL DEFAULT 'noise' COMMENT 'noise/sine',
  `f_lo` decimal(10,2) NOT NULL DEFAULT '150.00' COMMENT '频率下限 Hz',
  `f_hi` decimal(10,2) NOT NULL DEFAULT '7000.00' COMMENT '频率上限 Hz',
  `env_cut` decimal(10,2) NOT NULL DEFAULT '160.00' COMMENT '包络低通截止 Hz',
  `spread` decimal(6,4) NOT NULL DEFAULT '0.1500' COMMENT '电流扩散 0-1',
  `noise_level` decimal(6,4) NOT NULL DEFAULT '0.0000' COMMENT '背景噪声 0-1',
  `version_no` int NOT NULL DEFAULT '0' COMMENT '成功生成版本号',
  `status` varchar(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PROCESSING/READY/FAILED/DISABLED',
  `error_message` varchar(500) DEFAULT NULL COMMENT '最近一次生成失败原因',
  `generated_time` datetime DEFAULT NULL COMMENT '最近生成成功时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_delete` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_read_audio_code` (`audio_code`),
  KEY `idx_read_audio_status` (`status`,`is_delete`),
  KEY `idx_read_audio_source` (`source_asset_id`),
  KEY `idx_read_audio_output` (`output_asset_id`),
  CONSTRAINT `fk_read_audio_source` FOREIGN KEY (`source_asset_id`) REFERENCES `file_asset` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_read_audio_output` FOREIGN KEY (`output_asset_id`) REFERENCES `file_asset` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_read_audio_channels` CHECK (`n_channels` IN (2,4,8,16)),
  CONSTRAINT `chk_read_audio_carrier` CHECK (`carrier` IN ('noise','sine')),
  CONSTRAINT `chk_read_audio_status` CHECK (`status` IN ('DRAFT','PROCESSING','READY','FAILED','DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='点读音频库：上传原声并生成模拟声，供点读卡片引用';

-- 点读卡片挂接音频库（可重复执行：列已存在则跳过需人工确认）
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'hear_like_me'
    AND TABLE_NAME = 'read_aloud_item'
    AND COLUMN_NAME = 'audio_bank_id'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE `read_aloud_item`
     ADD COLUMN `audio_bank_id` bigint DEFAULT NULL COMMENT ''引用的点读音频库记录'' AFTER `image_asset_id`,
     ADD KEY `idx_read_item_audio_bank` (`audio_bank_id`),
     ADD CONSTRAINT `fk_read_item_audio_bank`
       FOREIGN KEY (`audio_bank_id`) REFERENCES `read_aloud_audio_bank` (`id`)
       ON DELETE SET NULL ON UPDATE CASCADE',
  'SELECT ''audio_bank_id already exists'' AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 菜单（admin 库）
USE `hear_like_me_admin`;

INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  2027, '点读音频库', 2020, 3, 'read-audio', 'hlm/read-aloud/audio/index', '', 'HlmReadAudio',
  1, 0, 'C', '0', '0', 'hlm:read:audio', 'radio',
  'admin', sysdate(), '', NULL, '点读模拟音频生产与管理'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2027);
