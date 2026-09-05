-- 挑战题目允许本地直传模拟音（不强制绑定音频库）
USE `hear_like_me`;
ALTER TABLE `hearing_challenge`
  MODIFY COLUMN `audio_bank_id` bigint DEFAULT NULL COMMENT '引用的挑战音频库记录；本地直传模拟音时可为空';
