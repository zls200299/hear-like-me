-- ============================================================================
-- 听音挑战答题记录 + 菜单迁至「听音挑战」
-- Database: hear_like_me / hear_like_me_admin
-- ============================================================================

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS `hear_like_me`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
USE `hear_like_me`;

CREATE TABLE IF NOT EXISTS `hearing_challenge_attempt` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '答题用户 user.id',
  `user_nickname` varchar(64) DEFAULT NULL COMMENT '答题时昵称快照',
  `question_id` bigint NOT NULL COMMENT '题目 hearing_challenge.id',
  `question_code` varchar(64) DEFAULT NULL COMMENT '题目编码快照',
  `question_title` varchar(150) DEFAULT NULL COMMENT '题目标题快照',
  `audio_bank_id` bigint DEFAULT NULL COMMENT '挑战音频库 ID，本地直传可为空',
  `audio_title` varchar(150) DEFAULT NULL COMMENT '音频名称快照（优先音频库标题，否则题目标题）',
  `audio_asset_id` bigint NOT NULL COMMENT '答题使用的音频 file_asset.id',
  `selected_channels` tinyint NOT NULL COMMENT '用户选择的通道数',
  `correct_channels` tinyint NOT NULL COMMENT '题目正确答案通道数',
  `is_correct` tinyint NOT NULL COMMENT '是否答对：0否 1是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '答题时间',
  PRIMARY KEY (`id`),
  KEY `idx_attempt_user_time` (`user_id`, `create_time`),
  KEY `idx_attempt_question_time` (`question_id`, `create_time`),
  KEY `idx_attempt_audio_bank` (`audio_bank_id`),
  KEY `idx_attempt_audio_asset` (`audio_asset_id`),
  KEY `idx_attempt_correct_time` (`is_correct`, `create_time`),
  CONSTRAINT `chk_attempt_selected_channels` CHECK (`selected_channels` IN (2,4,8,16)),
  CONSTRAINT `chk_attempt_correct_channels` CHECK (`correct_channels` IN (2,4,8,16)),
  CONSTRAINT `chk_attempt_is_correct` CHECK (`is_correct` IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='听音挑战答题记录';

-- 菜单：挑战记录挂到听音挑战下（从用户历史迁出）
CREATE DATABASE IF NOT EXISTS `hear_like_me_admin`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
USE `hear_like_me_admin`;

UPDATE `sys_menu`
SET `parent_id` = '2004',
    `order_num` = 3,
    `path` = 'record',
    `component` = 'hlm/challenge/record/index',
    `query` = '',
    `route_name` = 'HlmChallengeRecord',
    `perms` = 'hlm:challenge:record:list',
    `icon` = 'form',
    `remark` = '听音挑战答题记录'
WHERE `menu_id` = '2024';
