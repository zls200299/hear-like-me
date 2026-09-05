-- ============================================================================
-- 听音挑战 · 挑战统计菜单
-- Database: hear_like_me_admin
-- ============================================================================

SET NAMES utf8mb4;
USE `hear_like_me_admin`;

DELETE FROM sys_role_menu WHERE menu_id = 2028;
DELETE FROM sys_menu WHERE menu_id = 2028;

INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
) VALUES (
  '2028', '挑战统计', '2004', '4', 'stats', 'hlm/challenge/stats/index', '', 'HlmChallengeStats',
  1, 0, 'C', '0', '0', 'hlm:challenge:stats', 'chart',
  'admin', sysdate(), '', NULL, '听音挑战答题统计看板'
);
