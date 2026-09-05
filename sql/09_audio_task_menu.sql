-- ============================================================================
-- 音频任务菜单：占位改为运维页
-- Database: hear_like_me_admin
-- ============================================================================

SET NAMES utf8mb4;
USE `hear_like_me_admin`;

UPDATE `sys_menu`
SET `component` = 'hlm/audio-task/index',
    `query` = '',
    `route_name` = 'HlmAudioTask',
    `perms` = 'hlm:task:list',
    `remark` = '音频处理任务运维'
WHERE `menu_id` = '2016';
