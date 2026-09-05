-- ============================================================================
-- Hear Like Me - 业务后台菜单（增量脚本）
-- Database: hear_like_me_admin
-- 说明：在 01_hear_like_me_admin.sql 初始化后执行；可重复执行（先删后插）
-- P0：场景预设、示例音、挑战题目、文件资源、业务配置
-- P1/P2：占位菜单，组件指向 hlm/placeholder/index
-- ============================================================================

CREATE DATABASE IF NOT EXISTS `hear_like_me_admin`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
USE `hear_like_me_admin`;

-- 清理旧菜单（仅本脚本 ID 段 2000-2099）
DELETE FROM sys_role_menu WHERE menu_id BETWEEN 2000 AND 2099;
DELETE FROM sys_menu WHERE menu_id BETWEEN 2000 AND 2099;

-- 一级：业务管理
INSERT INTO sys_menu VALUES
('2000', '业务管理', '0', '1', 'hlm', NULL, '', '', 1, 0, 'M', '0', '0', '', 'education', 'admin', sysdate(), '', NULL, 'Hear Like Me 业务管理');

-- P0：听觉模拟
INSERT INTO sys_menu VALUES
('2001', '听觉模拟', '2000', '1', 'simulator', NULL, '', '', 1, 0, 'M', '0', '0', '', 'radio', 'admin', sysdate(), '', NULL, '听觉模拟配置'),
('2002', '场景预设', '2001', '1', 'scenario', 'hlm/simulator/scenario/index', '', 'HlmScenario', 1, 0, 'C', '0', '0', 'hlm:scenario:list', 'component', 'admin', sysdate(), '', NULL, 'P0-场景预设'),
('2003', '示例音',   '2001', '2', 'sample',   'hlm/simulator/sample/index',   '', 'HlmSample',   1, 0, 'C', '0', '0', 'hlm:sample:list',   'sound',     'admin', sysdate(), '', NULL, 'P0-示例音');

-- P0：听音挑战
INSERT INTO sys_menu VALUES
('2004', '听音挑战', '2000', '2', 'challenge', NULL, '', '', 1, 0, 'M', '0', '0', '', 'question', 'admin', sysdate(), '', NULL, '听音挑战'),
('2026', '挑战音频库', '2004', '1', 'audio', 'hlm/challenge/audio/index', '', 'HlmChallengeAudio', 1, 0, 'C', '0', '0', 'hlm:challenge:audio:list', 'radio', 'admin', sysdate(), '', NULL, '听音挑战音频生产与管理'),
('2005', '题目管理', '2004', '2', 'question', 'hlm/challenge/question/index', '', 'HlmChallengeQuestion', 1, 0, 'C', '0', '0', 'hlm:challenge:list', 'list', 'admin', sysdate(), '', NULL, 'P0-挑战题目');

-- P0：资源与配置
INSERT INTO sys_menu VALUES
('2006', '资源管理', '2000', '3', 'resource', NULL, '', '', 1, 0, 'M', '0', '0', '', 'upload', 'admin', sysdate(), '', NULL, '文件资源'),
('2007', '文件资源', '2006', '1', 'file', 'hlm/file/asset/index', '', 'HlmFileAsset', 1, 0, 'C', '0', '0', 'hlm:file:list', 'documentation', 'admin', sysdate(), '', NULL, 'P0-文件资源'),
('2008', '业务设置', '2000', '5', 'biz-settings', NULL, '', '', 1, 0, 'M', '0', '0', '', 'system', 'admin', sysdate(), '', NULL, '业务系统配置'),
('2009', '业务配置', '2008', '1', 'config', 'hlm/config/biz/index', '', 'HlmBizConfig', 1, 0, 'C', '0', '0', 'hlm:config:list', 'edit', 'admin', sysdate(), '', NULL, 'P0-业务配置');

-- P1：占位（用户、统计、科普、音频任务）
INSERT INTO sys_menu VALUES
('2010', '用户管理', '2000', '6', 'user-mgmt', NULL, '', '', 1, 0, 'M', '0', '0', '', 'user', 'admin', sysdate(), '', NULL, 'P1-小程序用户'),
('2011', '小程序用户', '2010', '1', 'mini-user', 'hlm/user/mini/index', '', 'HlmMiniUser', 1, 0, 'C', '0', '0', 'hlm:miniuser:list', 'peoples', 'admin', sysdate(), '', NULL, '小程序用户管理'),
('2012', '数据统计', '2000', '7', 'stats', 'hlm/placeholder/index', '{"phase":"P1","module":"stats"}', 'HlmStats', 1, 0, 'C', '0', '0', 'hlm:stats:list', 'chart', 'admin', sysdate(), '', NULL, 'P1-占位'),
('2013', '科普学习', '2000', '8', 'learn', NULL, '', '', 1, 0, 'M', '0', '0', '', 'documentation', 'admin', sysdate(), '', NULL, 'P1-科普'),
('2014', '内容分类', '2013', '1', 'category', 'hlm/placeholder/index', '{"phase":"P1","module":"content-category"}', 'HlmContentCategory', 1, 0, 'C', '0', '0', 'hlm:content:category', 'tree', 'admin', sysdate(), '', NULL, 'P1-占位'),
('2015', '文章管理', '2013', '2', 'article', 'hlm/placeholder/index', '{"phase":"P1","module":"content-article"}', 'HlmContentArticle', 1, 0, 'C', '0', '0', 'hlm:content:article', 'edit', 'admin', sysdate(), '', NULL, 'P1-占位'),
('2016', '音频任务', '2000', '9', 'audio-task', 'hlm/placeholder/index', '{"phase":"P1","module":"audio-task"}', 'HlmAudioTask', 1, 0, 'C', '0', '0', 'hlm:task:list', 'log', 'admin', sysdate(), '', NULL, 'P1-占位');

-- P2：点读管理 + 用户历史占位
INSERT INTO sys_menu VALUES
('2020', '点读管理', '2000', '4', 'read-aloud', NULL, '', '', 1, 0, 'M', '0', '0', '', 'table', 'admin', sysdate(), '', NULL, 'P2-点读'),
('2021', '点读分类', '2020', '1', 'read-category', 'hlm/read-aloud/category/index', '', 'HlmReadCategory', 1, 0, 'C', '0', '0', 'hlm:read:category', 'tree-table', 'admin', sysdate(), '', NULL, '点读分类'),
('2022', '点读卡片', '2020', '2', 'read-item', 'hlm/read-aloud/item/index', '', 'HlmReadItem', 1, 0, 'C', '0', '0', 'hlm:read:item', 'tab', 'admin', sysdate(), '', NULL, '点读卡片'),
('2027', '点读音频库', '2020', '3', 'read-audio', 'hlm/read-aloud/audio/index', '', 'HlmReadAudio', 1, 0, 'C', '0', '0', 'hlm:read:audio', 'radio', 'admin', sysdate(), '', NULL, '点读模拟音频生产与管理'),
('2023', '用户历史', '2000', '10', 'history', NULL, '', '', 1, 0, 'M', '0', '0', '', 'time', 'admin', sysdate(), '', NULL, 'P2-用户历史'),
('2024', '挑战记录', '2023', '1', 'challenge-history', 'hlm/placeholder/index', '{"phase":"P2","module":"challenge-history"}', 'HlmChallengeHistory', 1, 0, 'C', '0', '0', 'hlm:history:challenge', 'skill', 'admin', sysdate(), '', NULL, 'P2-占位'),
('2025', '模拟历史', '2023', '2', 'sim-history', 'hlm/placeholder/index', '{"phase":"P2","module":"sim-history"}', 'HlmSimHistory', 1, 0, 'C', '0', '0', 'hlm:history:sim', 'date', 'admin', sysdate(), '', NULL, 'P2-占位');

-- 普通角色(role_id=2)可见 P0 菜单（管理员 role_id=1 默认超权）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 2, menu_id FROM sys_menu WHERE menu_id BETWEEN 2000 AND 2009 OR menu_id = 2026;
