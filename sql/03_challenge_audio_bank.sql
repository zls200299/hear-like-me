-- 听音挑战“模拟音频库”增量迁移
-- 业务库：hear_like_me
USE `hear_like_me`;

CREATE TABLE IF NOT EXISTS `challenge_audio_bank` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `audio_code` varchar(64) NOT NULL COMMENT '音频唯一编码',
  `title` varchar(150) NOT NULL COMMENT '音频名称',
  `description` varchar(500) DEFAULT NULL COMMENT '素材说明',
  `source_asset_id` bigint NOT NULL COMMENT '原始音频 file_asset.id',
  `output_asset_id` bigint DEFAULT NULL COMMENT '生成后的模拟音频 file_asset.id',
  `processing_task_no` varchar(64) DEFAULT NULL COMMENT '最近一次处理任务编号',
  `n_channels` tinyint NOT NULL COMMENT '有效通道数，同时作为挑战正确答案',
  `carrier` varchar(20) NOT NULL DEFAULT 'noise',
  `f_lo` decimal(10,2) NOT NULL DEFAULT '150.00',
  `f_hi` decimal(10,2) NOT NULL DEFAULT '7000.00',
  `env_cut` decimal(10,2) NOT NULL DEFAULT '160.00',
  `spread` decimal(6,4) NOT NULL DEFAULT '0.1500',
  `noise_level` decimal(6,4) NOT NULL DEFAULT '0.0000',
  `version_no` int NOT NULL DEFAULT '0',
  `status` varchar(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PROCESSING/READY/FAILED/DISABLED',
  `error_message` varchar(500) DEFAULT NULL,
  `generated_time` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_delete` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_challenge_audio_code` (`audio_code`),
  KEY `idx_challenge_audio_status` (`status`,`is_delete`),
  KEY `idx_challenge_audio_source` (`source_asset_id`),
  KEY `idx_challenge_audio_output` (`output_asset_id`),
  CONSTRAINT `fk_challenge_audio_source` FOREIGN KEY (`source_asset_id`) REFERENCES `file_asset` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_challenge_audio_output` FOREIGN KEY (`output_asset_id`) REFERENCES `file_asset` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_challenge_audio_channels` CHECK (`n_channels` IN (2,4,8,16)),
  CONSTRAINT `chk_challenge_audio_carrier` CHECK (`carrier` IN ('noise','sine')),
  CONSTRAINT `chk_challenge_audio_status` CHECK (`status` IN ('DRAFT','PROCESSING','READY','FAILED','DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='听音挑战模拟音频库';

ALTER TABLE `hearing_challenge`
  ADD COLUMN `audio_bank_id` bigint DEFAULT NULL COMMENT '引用的模拟音频库记录' AFTER `description`,
  ADD KEY `idx_hearing_challenge_audio_bank` (`audio_bank_id`);

-- 将旧题目的现有音频作为一个已就绪的兼容素材迁入，不改变小程序当前播放内容。
INSERT INTO `challenge_audio_bank` (
  `audio_code`, `title`, `description`, `source_asset_id`, `output_asset_id`,
  `n_channels`, `carrier`, `f_lo`, `f_hi`, `env_cut`, `spread`, `noise_level`,
  `version_no`, `status`, `generated_time`, `is_delete`
)
SELECT CONCAT('legacy-', hc.id), CONCAT(hc.title, '（历史导入）'), hc.description,
       hc.audio_asset_id, hc.audio_asset_id, hc.n_channels, hc.carrier,
       hc.f_lo, hc.f_hi, hc.env_cut, hc.spread, hc.noise_level,
       1, 'READY', hc.update_time, 0
FROM `hearing_challenge` hc
LEFT JOIN `challenge_audio_bank` cab ON cab.audio_code = CONCAT('legacy-', hc.id)
WHERE hc.is_delete = 0 AND cab.id IS NULL;

UPDATE `hearing_challenge` hc
JOIN `challenge_audio_bank` cab ON cab.audio_code = CONCAT('legacy-', hc.id)
SET hc.audio_bank_id = cab.id
WHERE hc.audio_bank_id IS NULL;

ALTER TABLE `hearing_challenge`
  ADD CONSTRAINT `fk_hearing_challenge_audio_bank`
    FOREIGN KEY (`audio_bank_id`) REFERENCES `challenge_audio_bank` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE;

-- 后台菜单库：hear_like_me_admin
USE `hear_like_me_admin`;

DELETE FROM `sys_role_menu` WHERE `menu_id` = 2026;
DELETE FROM `sys_menu` WHERE `menu_id` = 2026;

UPDATE `sys_menu` SET `order_num` = '2' WHERE `menu_id` = 2005;
INSERT INTO `sys_menu` VALUES
('2026', '模拟音频库', '2004', '1', 'audio', 'hlm/challenge/audio/index', '', 'HlmChallengeAudio', 1, 0, 'C', '0', '0', 'hlm:challenge:audio:list', 'radio', 'admin', sysdate(), '', NULL, '挑战模拟音频生产与管理');

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 2, 2026 WHERE NOT EXISTS (
  SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 2 AND `menu_id` = 2026
);


