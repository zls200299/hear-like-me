-- ============================================================================
-- Hear Like Me - 业务数据库初始化脚本 v2.0
-- Target: MySQL 8.0+
-- Database: hear_like_me
--
-- 适配：scaffolding-v2 / jsj-mini
-- 当前策略：
--   1. 微信小程序先行
--   2. 登录表按脚手架实体预留，但一期可不启用登录
--   3. 用户音频/图片 V1 默认 LOCAL 本地服务器存储，后续可迁移 COS/OSS
--   4. 音频处理：Spring Boot + FFmpeg + Python Cochlear Vocoder
--   5. 点读支持“原音 / 模拟音 / 两者”三种模式，客户未确认前默认 ORIGINAL
--   6. 实时麦克风若采用小程序本地 DSP，不强制写入任务表
--
-- 注意：建议在空库首次执行。脚本使用 CREATE TABLE IF NOT EXISTS，
--       初始化数据使用 ON DUPLICATE KEY UPDATE，可重复执行而不清空业务数据。
-- ============================================================================

SET NAMES utf8mb4;
CREATE DATABASE IF NOT EXISTS `hear_like_me`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
USE `hear_like_me`;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================================
-- 1. 用户主表
--    对齐 jsj-mini 当前 com.zhs.model.User 字段。
--    一期不做登录时可以保持为空。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `user` (
    `id`                  BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID；兼容 MyBatis-Plus ASSIGN_ID 显式写入',
    `nickname`            VARCHAR(64) DEFAULT NULL COMMENT '用户昵称',
    `avatar`              VARCHAR(512) DEFAULT NULL COMMENT '头像地址',
    `bio`                 VARCHAR(255) DEFAULT NULL COMMENT '个人简介',
    `status`              TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1正常',
    `source_type`         TINYINT NOT NULL DEFAULT 3 COMMENT '来源：1后台创建 2手机号注册 3微信小程序 4微信公众号/网页 5邮箱注册',
    `register_time`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    `last_active_time`    DATETIME DEFAULT NULL COMMENT '最后活跃时间',
    `create_time`         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`           TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删',
    PRIMARY KEY (`id`),
    KEY `idx_user_status_delete` (`status`, `is_delete`),
    KEY `idx_user_register_time` (`register_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='小程序/网站统一用户主表';

-- ============================================================================
-- 2. 用户微信敏感身份信息
--    当前字段兼容脚手架 UserSensitiveInfo。
--    小程序和网页统一账号若最终确定，建议后续按 UnionID/多身份进一步扩展。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `user_sensitive_info` (
    `id`                  BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`             BIGINT NOT NULL COMMENT '关联 user.id',
    `open_id`             VARCHAR(128) DEFAULT NULL COMMENT '微信 OpenID',
    `union_id`            VARCHAR(128) DEFAULT NULL COMMENT '微信 UnionID',
    `mini_app_id`         VARCHAR(64) DEFAULT NULL COMMENT '微信小程序 AppID',
    `official_app_id`     VARCHAR(64) DEFAULT NULL COMMENT '公众号/网页相关 AppID',
    `is_delete`           TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志：0未删除 1已删除',
    `create_time`         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sensitive_user_id` (`user_id`),
    UNIQUE KEY `uk_sensitive_open_id` (`open_id`),
    KEY `idx_sensitive_union_id` (`union_id`),
    CONSTRAINT `fk_sensitive_user`
        FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户微信敏感身份信息';

-- ============================================================================
-- 3. 用户登录会话
--    脚手架 LoginInterceptor 当前会读取该表；AuthService 当前又把 token 写 Redis。
--    因此表先补齐，但真正启用登录前需把代码统一为“Redis”或“DB Session”一种方案。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `user_login_session` (
    `id`                  BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`             BIGINT NOT NULL COMMENT '用户ID',
    `token`               VARCHAR(128) NOT NULL COMMENT '登录 Token',
    `login_time`          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    `expire_time`         DATETIME NOT NULL COMMENT '过期时间',
    `device_type`         VARCHAR(32) DEFAULT NULL COMMENT '设备类型：MINIPROGRAM/WEB/OTHER',
    `client_ip`           VARCHAR(64) DEFAULT NULL COMMENT '客户端IP',
    `status`              TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0失效 1有效',
    `is_delete`           TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志：0未删除 1已删除',
    `create_time`         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_login_token` (`token`),
    KEY `idx_login_user_status` (`user_id`, `status`, `is_delete`),
    KEY `idx_login_expire_time` (`expire_time`),
    CONSTRAINT `fk_login_session_user`
        FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户登录会话';

-- ============================================================================
-- 4. 统一文件资源表
--    文件本体不存 MySQL；MySQL 只保存本地相对路径或对象存储 Key。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `file_asset` (
    `id`                  BIGINT NOT NULL AUTO_INCREMENT COMMENT '文件ID',
    `owner_user_id`       BIGINT DEFAULT NULL COMMENT '文件所属用户；未登录/公共素材可为空',
    `parent_asset_id`     BIGINT DEFAULT NULL COMMENT '派生文件的上游文件ID',

    `asset_type`          VARCHAR(32) NOT NULL COMMENT 'AUDIO_SOURCE/AUDIO_NORMALIZED/AUDIO_OUTPUT/SAMPLE_AUDIO/READ_IMAGE/READ_AUDIO/READ_AUDIO_PROCESSED/CONTENT_IMAGE/OTHER',
    `storage_provider`    VARCHAR(20) NOT NULL DEFAULT 'LOCAL' COMMENT 'LOCAL/COS/OSS/S3',
    `bucket_name`         VARCHAR(128) NOT NULL DEFAULT '' COMMENT '对象存储 Bucket；LOCAL 时为空字符串',
    `object_key`          VARCHAR(512) NOT NULL COMMENT '对象存储 Key 或服务器本地相对路径',

    `original_filename`   VARCHAR(255) DEFAULT NULL COMMENT '原始文件名',
    `file_ext`            VARCHAR(20) DEFAULT NULL COMMENT '扩展名，不含点',
    `mime_type`           VARCHAR(100) DEFAULT NULL COMMENT 'MIME 类型',
    `file_size`           BIGINT DEFAULT NULL COMMENT '文件大小（字节）',
    `sha256`              CHAR(64) DEFAULT NULL COMMENT 'SHA-256',

    `duration_ms`         BIGINT DEFAULT NULL COMMENT '音频时长（毫秒）',
    `sample_rate`         INT DEFAULT NULL COMMENT '音频采样率 Hz',
    `audio_channels`      TINYINT DEFAULT NULL COMMENT '音频声道数',
    `bit_depth`           TINYINT DEFAULT NULL COMMENT 'PCM 位深',
    `audio_codec`         VARCHAR(50) DEFAULT NULL COMMENT '编码：pcm_s16le/aac/mp3 等',

    `image_width`         INT DEFAULT NULL COMMENT '图片宽度 px',
    `image_height`        INT DEFAULT NULL COMMENT '图片高度 px',

    `access_mode`         VARCHAR(16) NOT NULL DEFAULT 'PRIVATE' COMMENT 'PRIVATE/PUBLIC',
    `status`              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/EXPIRED/DELETED',
    `expire_time`         DATETIME DEFAULT NULL COMMENT '临时文件自动过期时间；NULL=长期保存',
    `delete_time`         DATETIME DEFAULT NULL COMMENT '软删除时间',
    `create_time`         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`           TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_file_storage_object` (`storage_provider`, `bucket_name`, `object_key`),
    KEY `idx_file_owner_created` (`owner_user_id`, `create_time`),
    KEY `idx_file_asset_type` (`asset_type`),
    KEY `idx_file_parent_asset` (`parent_asset_id`),
    KEY `idx_file_sha256` (`sha256`),
    KEY `idx_file_status_expire` (`status`, `expire_time`),

    CONSTRAINT `fk_file_owner_user`
        FOREIGN KEY (`owner_user_id`) REFERENCES `user` (`id`)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT `fk_file_parent_asset`
        FOREIGN KEY (`parent_asset_id`) REFERENCES `file_asset` (`id`)
        ON DELETE SET NULL ON UPDATE CASCADE,

    CONSTRAINT `chk_file_access_mode` CHECK (`access_mode` IN ('PRIVATE','PUBLIC')),
    CONSTRAINT `chk_file_status` CHECK (`status` IN ('ACTIVE','EXPIRED','DELETED')),
    CONSTRAINT `chk_file_size` CHECK (`file_size` IS NULL OR `file_size` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='统一文件资源表';

-- ============================================================================
-- 5. 人工耳蜗场景预设
-- ============================================================================
CREATE TABLE IF NOT EXISTS `scenario_preset` (
    `id`                  BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `scenario_code`       VARCHAR(32) NOT NULL COMMENT 'quiet/restaurant/phone/music/tone/minimal',
    `name_cn`             VARCHAR(100) NOT NULL COMMENT '中文名',
    `name_en`             VARCHAR(100) DEFAULT NULL COMMENT '英文名',
    `description_cn`      VARCHAR(500) DEFAULT NULL COMMENT '中文说明',
    `description_en`      VARCHAR(500) DEFAULT NULL COMMENT '英文说明',
    `icon`                VARCHAR(100) DEFAULT NULL COMMENT '图标 key',

    `n_channels`          TINYINT NOT NULL DEFAULT 8 COMMENT '有效通道数 1-22',
    `carrier`             VARCHAR(20) NOT NULL DEFAULT 'noise' COMMENT 'noise/sine',
    `f_lo`                DECIMAL(10,2) NOT NULL DEFAULT 150.00 COMMENT '频率下限 Hz',
    `f_hi`                DECIMAL(10,2) NOT NULL DEFAULT 7000.00 COMMENT '频率上限 Hz',
    `env_cut`             DECIMAL(10,2) NOT NULL DEFAULT 160.00 COMMENT '包络低通截止 Hz',
    `spread`              DECIMAL(6,4) NOT NULL DEFAULT 0.1500 COMMENT '电流扩散 0-1',
    `noise_level`         DECIMAL(6,4) NOT NULL DEFAULT 0.0000 COMMENT '背景噪声 0-1',
    `env_amp`             DECIMAL(8,4) NOT NULL DEFAULT 2.6000 COMMENT '包络增益',
    `wet_mix`             DECIMAL(6,4) NOT NULL DEFAULT 0.9000 COMMENT '处理后声音混合比例 0-1',
    `compress_enabled`    TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用动态压缩',
    `normalize_peak`      DECIMAL(6,4) DEFAULT 0.8900 COMMENT '归一化峰值；NULL=关闭',

    `default_sample_code` VARCHAR(32) DEFAULT NULL COMMENT 'vowel/tone/melody',
    `sort_order`          INT NOT NULL DEFAULT 0 COMMENT '排序',
    `enabled`             TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    `create_time`         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`           TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_scenario_code` (`scenario_code`),
    KEY `idx_scenario_enabled_sort` (`enabled`, `sort_order`),

    CONSTRAINT `chk_scenario_channels` CHECK (`n_channels` BETWEEN 1 AND 22),
    CONSTRAINT `chk_scenario_carrier` CHECK (`carrier` IN ('noise','sine')),
    CONSTRAINT `chk_scenario_frequency` CHECK (`f_lo` > 0 AND `f_hi` > `f_lo`),
    CONSTRAINT `chk_scenario_env` CHECK (`env_cut` BETWEEN 20 AND 500),
    CONSTRAINT `chk_scenario_spread` CHECK (`spread` BETWEEN 0 AND 1),
    CONSTRAINT `chk_scenario_noise` CHECK (`noise_level` BETWEEN 0 AND 1),
    CONSTRAINT `chk_scenario_wet` CHECK (`wet_mix` BETWEEN 0 AND 1),
    CONSTRAINT `chk_scenario_normalize` CHECK (`normalize_peak` IS NULL OR (`normalize_peak` > 0 AND `normalize_peak` <= 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='人工耳蜗场景预设';

-- ============================================================================
-- 6. 内置示例音
-- ============================================================================
CREATE TABLE IF NOT EXISTS `sample_audio` (
    `id`                  BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `sample_code`         VARCHAR(32) NOT NULL COMMENT 'vowel/tone/melody',
    `name_cn`             VARCHAR(100) NOT NULL COMMENT '中文名',
    `name_en`             VARCHAR(100) DEFAULT NULL COMMENT '英文名',
    `description_cn`      VARCHAR(500) DEFAULT NULL COMMENT '中文说明',
    `description_en`      VARCHAR(500) DEFAULT NULL COMMENT '英文说明',
    `asset_id`            BIGINT DEFAULT NULL COMMENT '预生成音频文件；Python 运行时生成时可为空',
    `generator_type`      VARCHAR(24) NOT NULL DEFAULT 'PYTHON_GENERATED' COMMENT 'PREGENERATED/PYTHON_GENERATED',
    `sort_order`          INT NOT NULL DEFAULT 0 COMMENT '排序',
    `enabled`             TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    `create_time`         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`           TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sample_code` (`sample_code`),
    KEY `idx_sample_asset` (`asset_id`),
    KEY `idx_sample_enabled_sort` (`enabled`, `sort_order`),
    CONSTRAINT `fk_sample_asset`
        FOREIGN KEY (`asset_id`) REFERENCES `file_asset` (`id`)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT `chk_sample_generator` CHECK (`generator_type` IN ('PREGENERATED','PYTHON_GENERATED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='内置示例音';

-- ============================================================================
-- 7. 音频处理任务
--    一条记录就是一次“输入 + 参数 -> 输出”。
--    该表本身即可作为以后用户的音频处理历史记录。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `audio_processing_task` (
    `id`                      BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `task_no`                 VARCHAR(64) NOT NULL COMMENT '对外任务编号 UUID/雪花ID',
    `user_id`                 BIGINT DEFAULT NULL COMMENT '所属用户；一期未登录可为空',

    `source_type`             VARCHAR(24) NOT NULL COMMENT 'SAMPLE/UPLOAD/RECORDING/READ_ALOUD',
    `sample_code`             VARCHAR(32) DEFAULT NULL COMMENT 'source_type=SAMPLE 时使用',
    `scenario_code`           VARCHAR(32) DEFAULT NULL COMMENT '场景代码；自定义参数时可为空',

    `source_asset_id`         BIGINT DEFAULT NULL COMMENT '原始输入文件；内置 Python 生成示例时可为空',
    `normalized_asset_id`     BIGINT DEFAULT NULL COMMENT 'FFmpeg 标准化后的中间 WAV',
    `output_asset_id`         BIGINT DEFAULT NULL COMMENT 'Cochlear Vocoder 输出文件',

    `n_channels`              TINYINT NOT NULL DEFAULT 8 COMMENT '有效通道数 1-22',
    `carrier`                 VARCHAR(20) NOT NULL DEFAULT 'noise' COMMENT 'noise/sine',
    `f_lo`                    DECIMAL(10,2) NOT NULL DEFAULT 150.00 COMMENT 'Hz',
    `f_hi`                    DECIMAL(10,2) NOT NULL DEFAULT 7000.00 COMMENT 'Hz',
    `env_cut`                 DECIMAL(10,2) NOT NULL DEFAULT 160.00 COMMENT 'Hz',
    `spread`                  DECIMAL(6,4) NOT NULL DEFAULT 0.1500 COMMENT '0-1',
    `noise_level`             DECIMAL(6,4) NOT NULL DEFAULT 0.0000 COMMENT '0-1',
    `env_amp`                 DECIMAL(8,4) NOT NULL DEFAULT 2.6000 COMMENT '包络增益',
    `wet_mix`                 DECIMAL(6,4) NOT NULL DEFAULT 0.9000 COMMENT '处理后声音混合比例 0-1',
    `compress_enabled`        TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用动态压缩',
    `normalize_peak`          DECIMAL(6,4) DEFAULT 0.8900 COMMENT '归一化峰值；NULL=关闭',
    `random_seed`             BIGINT DEFAULT NULL COMMENT '随机种子，便于复现实验',
    `algorithm_version`       VARCHAR(64) NOT NULL DEFAULT 'cochlear-vocoder-v1' COMMENT '算法版本',

    `task_status`             VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PROCESSING/SUCCESS/FAILED/CANCELLED',
    `progress`                TINYINT NOT NULL DEFAULT 0 COMMENT '0-100',
    `retry_count`             TINYINT NOT NULL DEFAULT 0 COMMENT '重试次数',

    `clarity_score`           TINYINT DEFAULT NULL COMMENT '清晰度参考分 0-100',
    `clarity_grade`           VARCHAR(32) DEFAULT NULL COMMENT '几乎听不懂/很吃力/大致能懂/比较清楚/接近清晰',
    `spectral_score`          DECIMAL(6,4) DEFAULT NULL COMMENT '频谱细节参考 0-1',
    `pitch_score`             DECIMAL(6,4) DEFAULT NULL COMMENT '音高线索参考 0-1',
    `noise_margin`            DECIMAL(6,4) DEFAULT NULL COMMENT '噪声余量参考 0-1',

    `error_code`              VARCHAR(64) DEFAULT NULL COMMENT '错误码',
    `error_message`           VARCHAR(1000) DEFAULT NULL COMMENT '错误信息',
    `queue_wait_ms`           BIGINT DEFAULT NULL COMMENT '排队等待毫秒',
    `processing_ms`           BIGINT DEFAULT NULL COMMENT '实际处理耗时毫秒',
    `processing_started_time` DATETIME DEFAULT NULL COMMENT '处理开始时间',
    `processing_finished_time` DATETIME DEFAULT NULL COMMENT '处理结束时间',

    `create_time`             DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`             DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`               TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除；可用于用户删除历史',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_audio_task_no` (`task_no`),
    KEY `idx_audio_task_user_created` (`user_id`, `create_time`),
    KEY `idx_audio_task_status_created` (`task_status`, `create_time`),
    KEY `idx_audio_task_source_asset` (`source_asset_id`),
    KEY `idx_audio_task_output_asset` (`output_asset_id`),
    KEY `idx_audio_task_scenario` (`scenario_code`),

    CONSTRAINT `fk_audio_task_user`
        FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT `fk_audio_task_source_asset`
        FOREIGN KEY (`source_asset_id`) REFERENCES `file_asset` (`id`)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT `fk_audio_task_normalized_asset`
        FOREIGN KEY (`normalized_asset_id`) REFERENCES `file_asset` (`id`)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT `fk_audio_task_output_asset`
        FOREIGN KEY (`output_asset_id`) REFERENCES `file_asset` (`id`)
        ON DELETE SET NULL ON UPDATE CASCADE,

    CONSTRAINT `chk_task_source_type` CHECK (`source_type` IN ('SAMPLE','UPLOAD','RECORDING','READ_ALOUD')),
    CONSTRAINT `chk_task_status` CHECK (`task_status` IN ('PENDING','PROCESSING','SUCCESS','FAILED','CANCELLED')),
    CONSTRAINT `chk_task_progress` CHECK (`progress` BETWEEN 0 AND 100),
    CONSTRAINT `chk_task_channels` CHECK (`n_channels` BETWEEN 1 AND 22),
    CONSTRAINT `chk_task_carrier` CHECK (`carrier` IN ('noise','sine')),
    CONSTRAINT `chk_task_frequency` CHECK (`f_lo` > 0 AND `f_hi` > `f_lo`),
    CONSTRAINT `chk_task_env` CHECK (`env_cut` BETWEEN 20 AND 500),
    CONSTRAINT `chk_task_spread` CHECK (`spread` BETWEEN 0 AND 1),
    CONSTRAINT `chk_task_noise` CHECK (`noise_level` BETWEEN 0 AND 1),
    CONSTRAINT `chk_task_wet` CHECK (`wet_mix` BETWEEN 0 AND 1),
    CONSTRAINT `chk_task_normalize` CHECK (`normalize_peak` IS NULL OR (`normalize_peak` > 0 AND `normalize_peak` <= 1)),
    CONSTRAINT `chk_task_clarity` CHECK (`clarity_score` IS NULL OR `clarity_score` BETWEEN 0 AND 100),
    CONSTRAINT `chk_task_spectral` CHECK (`spectral_score` IS NULL OR `spectral_score` BETWEEN 0 AND 1),
    CONSTRAINT `chk_task_pitch` CHECK (`pitch_score` IS NULL OR `pitch_score` BETWEEN 0 AND 1),
    CONSTRAINT `chk_task_noise_margin` CHECK (`noise_margin` IS NULL OR `noise_margin` BETWEEN 0 AND 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='音频处理任务/历史记录';

-- ============================================================================
-- 8. 音频处理任务事件日志
-- ============================================================================
CREATE TABLE IF NOT EXISTS `audio_processing_task_event` (
    `id`                  BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `task_id`             BIGINT NOT NULL COMMENT '任务ID',
    `event_type`          VARCHAR(32) NOT NULL COMMENT 'CREATED/NORMALIZING/PROCESSING/SUCCESS/FAILED/RETRY/CANCELLED',
    `stage`               VARCHAR(50) DEFAULT NULL COMMENT 'UPLOAD/FFMPEG/VOCODER/STORAGE/OTHER',
    `progress`            TINYINT DEFAULT NULL COMMENT '0-100',
    `message`             VARCHAR(1000) DEFAULT NULL COMMENT '事件描述',
    `detail_json`         JSON NULL COMMENT '扩展数据',
    `create_time`         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_task_event_task_created` (`task_id`, `create_time`),
    KEY `idx_task_event_type` (`event_type`),
    CONSTRAINT `fk_task_event_task`
        FOREIGN KEY (`task_id`) REFERENCES `audio_processing_task` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `chk_task_event_progress` CHECK (`progress` IS NULL OR `progress` BETWEEN 0 AND 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='音频处理任务事件日志';

-- ============================================================================
-- 9. 科普分类
-- ============================================================================
CREATE TABLE IF NOT EXISTS `content_category` (
    `id`                  BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `category_code`       VARCHAR(64) NOT NULL COMMENT 'how-it-works/journey/limitations/children/disclaimer',
    `name_cn`             VARCHAR(100) NOT NULL COMMENT '中文名',
    `name_en`             VARCHAR(100) DEFAULT NULL COMMENT '英文名',
    `description_cn`      VARCHAR(500) DEFAULT NULL COMMENT '中文说明',
    `description_en`      VARCHAR(500) DEFAULT NULL COMMENT '英文说明',
    `sort_order`          INT NOT NULL DEFAULT 0 COMMENT '排序',
    `enabled`             TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    `create_time`         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`           TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_content_category_code` (`category_code`),
    KEY `idx_content_category_enabled_sort` (`enabled`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='科普内容分类';

-- ============================================================================
-- 10. 科普文章/页面
--     同一 slug 可分别保存 zh-CN / en-US。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `content_article` (
    `id`                  BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `category_id`         BIGINT NOT NULL COMMENT '分类ID',
    `slug`                VARCHAR(100) NOT NULL COMMENT '页面/文章唯一标识',
    `lang`                VARCHAR(10) NOT NULL DEFAULT 'zh-CN' COMMENT 'zh-CN/en-US',
    `title`               VARCHAR(255) NOT NULL COMMENT '标题',
    `subtitle`            VARCHAR(500) DEFAULT NULL COMMENT '副标题',
    `summary`             TEXT NULL COMMENT '摘要',
    `content_format`      VARCHAR(20) NOT NULL DEFAULT 'MARKDOWN' COMMENT 'MARKDOWN/HTML/JSON',
    `content_body`        LONGTEXT NOT NULL COMMENT '正文',
    `cover_asset_id`      BIGINT DEFAULT NULL COMMENT '封面/插图文件ID',
    `status`              VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/OFFLINE',
    `sort_order`          INT NOT NULL DEFAULT 0 COMMENT '排序',
    `published_time`      DATETIME DEFAULT NULL COMMENT '发布时间',
    `create_time`         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`           TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_content_slug_lang` (`slug`, `lang`),
    KEY `idx_content_article_category` (`category_id`),
    KEY `idx_content_article_status_sort` (`status`, `sort_order`),
    KEY `idx_content_article_cover` (`cover_asset_id`),

    CONSTRAINT `fk_content_article_category`
        FOREIGN KEY (`category_id`) REFERENCES `content_category` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_content_article_cover`
        FOREIGN KEY (`cover_asset_id`) REFERENCES `file_asset` (`id`)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT `chk_content_lang` CHECK (`lang` IN ('zh-CN','en-US')),
    CONSTRAINT `chk_content_format` CHECK (`content_format` IN ('MARKDOWN','HTML','JSON')),
    CONSTRAINT `chk_content_status` CHECK (`status` IN ('DRAFT','PUBLISHED','OFFLINE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='科普文章/页面';

-- ============================================================================
-- 11. 图片点读分类
-- ============================================================================
CREATE TABLE IF NOT EXISTS `read_aloud_category` (
    `id`                  BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `category_code`       VARCHAR(64) NOT NULL COMMENT 'daily/fruit/animal/transport 等',
    `name_cn`             VARCHAR(100) NOT NULL COMMENT '中文名',
    `name_en`             VARCHAR(100) DEFAULT NULL COMMENT '英文名',
    `cover_asset_id`      BIGINT DEFAULT NULL COMMENT '分类封面',
    `sort_order`          INT NOT NULL DEFAULT 0 COMMENT '排序',
    `enabled`             TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    `create_time`         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`           TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_read_category_code` (`category_code`),
    KEY `idx_read_category_enabled_sort` (`enabled`, `sort_order`),
    KEY `idx_read_category_cover` (`cover_asset_id`),
    CONSTRAINT `fk_read_category_cover`
        FOREIGN KEY (`cover_asset_id`) REFERENCES `file_asset` (`id`)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='图片点读分类';

-- ============================================================================
-- 12. 图片点读内容
--     当前按“一张图片 = 一个点读内容”设计。
--     play_mode 预留：
--       ORIGINAL  直接播放后台配置的普通中文音频
--       PROCESSED 播放 Cochlear Vocoder 模拟音频
--       BOTH      前端可在原音/模拟音之间切换
--     如果客户后续明确“一张图多个热点”，再新增 read_aloud_hotspot 表。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `read_aloud_item` (
    `id`                       BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `category_id`              BIGINT DEFAULT NULL COMMENT '分类ID',
    `item_code`                VARCHAR(64) NOT NULL COMMENT '唯一业务编码',
    `title_cn`                 VARCHAR(100) NOT NULL COMMENT '中文标题',
    `title_en`                 VARCHAR(100) DEFAULT NULL COMMENT '英文标题',
    `speech_text_cn`           VARCHAR(500) DEFAULT NULL COMMENT '点读中文文字，如“苹果”',
    `description_cn`           VARCHAR(500) DEFAULT NULL COMMENT '中文说明',
    `description_en`           VARCHAR(500) DEFAULT NULL COMMENT '英文说明',

    `image_asset_id`           BIGINT DEFAULT NULL COMMENT '点读图片',
    `audio_asset_id`           BIGINT DEFAULT NULL COMMENT '普通中文点读音频',
    `processed_audio_asset_id` BIGINT DEFAULT NULL COMMENT '预生成的人工耳蜗模拟音频；可为空',
    `play_mode`                VARCHAR(20) NOT NULL DEFAULT 'ORIGINAL' COMMENT 'ORIGINAL/PROCESSED/BOTH',
    `default_scenario_code`    VARCHAR(32) DEFAULT NULL COMMENT '需要模拟音时默认使用的场景代码',

    `status`                   VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/OFFLINE',
    `sort_order`               INT NOT NULL DEFAULT 0 COMMENT '排序',
    `create_time`              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`                TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_read_item_code` (`item_code`),
    KEY `idx_read_item_category` (`category_id`),
    KEY `idx_read_item_status_sort` (`status`, `sort_order`),
    KEY `idx_read_item_image` (`image_asset_id`),
    KEY `idx_read_item_audio` (`audio_asset_id`),
    KEY `idx_read_item_processed_audio` (`processed_audio_asset_id`),

    CONSTRAINT `fk_read_item_category`
        FOREIGN KEY (`category_id`) REFERENCES `read_aloud_category` (`id`)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT `fk_read_item_image`
        FOREIGN KEY (`image_asset_id`) REFERENCES `file_asset` (`id`)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT `fk_read_item_audio`
        FOREIGN KEY (`audio_asset_id`) REFERENCES `file_asset` (`id`)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT `fk_read_item_processed_audio`
        FOREIGN KEY (`processed_audio_asset_id`) REFERENCES `file_asset` (`id`)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT `chk_read_play_mode` CHECK (`play_mode` IN ('ORIGINAL','PROCESSED','BOTH')),
    CONSTRAINT `chk_read_item_status` CHECK (`status` IN ('DRAFT','PUBLISHED','OFFLINE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='图片点读内容';

-- ============================================================================
-- 13. 业务系统配置
--     与后台框架 hear_like_me_admin.sys_config 分开，专门存 Hear Like Me 业务参数。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `system_config` (
    `id`                  BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `config_key`          VARCHAR(128) NOT NULL COMMENT '配置键',
    `config_value`        TEXT NOT NULL COMMENT '配置值',
    `value_type`          VARCHAR(20) NOT NULL DEFAULT 'STRING' COMMENT 'STRING/INT/DECIMAL/BOOL/JSON',
    `description`         VARCHAR(500) DEFAULT NULL COMMENT '说明',
    `enabled`             TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    `create_time`         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_system_config_key` (`config_key`),
    CONSTRAINT `chk_system_config_value_type` CHECK (`value_type` IN ('STRING','INT','DECIMAL','BOOL','JSON'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Hear Like Me 业务系统配置';

-- ============================================================================
-- 初始化：六个场景预设
-- 参数与当前 cochlear_vocoder.py 场景保持一致；env_amp/wet_mix 使用算法默认值。
-- ============================================================================
INSERT INTO `scenario_preset`
(`scenario_code`,`name_cn`,`name_en`,`description_cn`,`description_en`,`icon`,
 `n_channels`,`carrier`,`f_lo`,`f_hi`,`env_cut`,`spread`,`noise_level`,`env_amp`,`wet_mix`,
 `compress_enabled`,`normalize_peak`,`default_sample_code`,`sort_order`,`enabled`,`is_delete`)
VALUES
('quiet','安静对话','Quiet chat','模拟安静环境中的人工耳蜗语音体验。','Cochlear-implant speech simulation in a quiet setting.','speech',8,'noise',150,7000,160,0.1500,0.0000,2.6000,0.9000,1,0.8900,'vowel',10,1,0),
('restaurant','嘈杂餐厅','Noisy restaurant','加入较强环境噪声和电流扩散，模拟嘈杂多人环境。','Adds stronger background noise and current spread.','restaurant',8,'noise',150,7000,160,0.4000,0.5500,2.6000,0.9000,1,0.8900,'vowel',20,1,0),
('phone','电话通话','Phone call','使用 300–3400 Hz 窄频带模拟电话声音。','Uses a narrow 300–3400 Hz telephone band.','phone',8,'noise',300,3400,160,0.1000,0.1500,2.6000,0.9000,1,0.8900,'vowel',30,1,0),
('music','听音乐','Music','使用简单旋律体验人工耳蜗下的音乐和音高损失。','Demonstrates music and pitch limitations.','music',8,'noise',80,8000,220,0.2500,0.0000,2.6000,0.9000,1,0.8900,'melody',40,1,0),
('tone','声调语言','Tonal language','使用上升/下降声调体验声调语言中的音高线索损失。','Demonstrates pitch-cue loss in tonal language.','tone',8,'noise',150,7000,120,0.2000,0.1000,2.6000,0.9000,1,0.8900,'tone',50,1,0),
('minimal','仅 4 通道','Only 4 channels','仅使用 4 个通道，突出通道数较少时的信息损失。','Uses only four channels to emphasize spectral information loss.','minimal',4,'noise',150,7000,160,0.0000,0.0000,2.6000,0.9000,1,0.8900,'vowel',60,1,0)
ON DUPLICATE KEY UPDATE
`name_cn`=VALUES(`name_cn`),`name_en`=VALUES(`name_en`),`description_cn`=VALUES(`description_cn`),
`description_en`=VALUES(`description_en`),`icon`=VALUES(`icon`),`n_channels`=VALUES(`n_channels`),
`carrier`=VALUES(`carrier`),`f_lo`=VALUES(`f_lo`),`f_hi`=VALUES(`f_hi`),`env_cut`=VALUES(`env_cut`),
`spread`=VALUES(`spread`),`noise_level`=VALUES(`noise_level`),`env_amp`=VALUES(`env_amp`),
`wet_mix`=VALUES(`wet_mix`),`compress_enabled`=VALUES(`compress_enabled`),
`normalize_peak`=VALUES(`normalize_peak`),`default_sample_code`=VALUES(`default_sample_code`),
`sort_order`=VALUES(`sort_order`),`enabled`=VALUES(`enabled`),`is_delete`=0;

-- ============================================================================
-- 初始化：内置示例音
-- ============================================================================
INSERT INTO `sample_audio`
(`sample_code`,`name_cn`,`name_en`,`description_cn`,`description_en`,`asset_id`,`generator_type`,`sort_order`,`enabled`,`is_delete`)
VALUES
('vowel','语音 · 元音 /a/ /i/ /u/','Speech · synthesized vowels /a-i-u/','用于体验不同通道数量对语音可懂度的影响。','Synthesized vowels for speech intelligibility demonstration.',NULL,'PYTHON_GENERATED',10,1,0),
('tone','声调 · 上升 / 下降','Tone · rising vs falling','用于体验人工耳蜗对声调和音高线索的影响。','Demonstrates rising and falling pitch cues.',NULL,'PYTHON_GENERATED',20,1,0),
('melody','音乐 · 简单旋律','Music · simple melody','用于体验旋律和音高信息在声码器下的损失。','Demonstrates melody and pitch degradation.',NULL,'PYTHON_GENERATED',30,1,0)
ON DUPLICATE KEY UPDATE
`name_cn`=VALUES(`name_cn`),`name_en`=VALUES(`name_en`),`description_cn`=VALUES(`description_cn`),
`description_en`=VALUES(`description_en`),`generator_type`=VALUES(`generator_type`),
`sort_order`=VALUES(`sort_order`),`enabled`=VALUES(`enabled`),`is_delete`=0;

-- ============================================================================
-- 初始化：科普分类
-- ============================================================================
INSERT INTO `content_category`
(`category_code`,`name_cn`,`name_en`,`description_cn`,`description_en`,`sort_order`,`enabled`,`is_delete`)
VALUES
('how-it-works','人工耳蜗如何工作','How it works','介绍分频、包络提取、载体重建与电刺激的基本过程。','Explains band splitting, envelope extraction, resynthesis and stimulation.',10,1,0),
('journey','声音的旅程','Journey of sound','从空气振动到处理器、电极、听觉神经和大脑。','From sound in air to processor, electrodes, auditory nerve and brain.',20,1,0),
('limitations','人工耳蜗的局限','Limitations','介绍音乐、音高、声调语言、噪声、定位等常见限制。','Introduces common limitations such as music, pitch, noise and localization.',30,1,0),
('children','儿童听力','Deaf children','介绍早筛、诊断、干预和长期支持。','Introduces screening, diagnosis, intervention and long-term support.',40,1,0),
('disclaimer','重要说明','Disclaimer','教育模拟及医学免责声明。','Educational simulation and medical disclaimer.',50,1,0)
ON DUPLICATE KEY UPDATE
`name_cn`=VALUES(`name_cn`),`name_en`=VALUES(`name_en`),`description_cn`=VALUES(`description_cn`),
`description_en`=VALUES(`description_en`),`sort_order`=VALUES(`sort_order`),`enabled`=VALUES(`enabled`),`is_delete`=0;

-- ============================================================================
-- 初始化：点读分类
-- ============================================================================
INSERT INTO `read_aloud_category`
(`category_code`,`name_cn`,`name_en`,`sort_order`,`enabled`,`is_delete`)
VALUES
('daily','日常生活','Daily life',10,1,0),
('fruit','水果','Fruit',20,1,0),
('animal','动物','Animals',30,1,0),
('transport','交通工具','Transport',40,1,0)
ON DUPLICATE KEY UPDATE
`name_cn`=VALUES(`name_cn`),`name_en`=VALUES(`name_en`),`sort_order`=VALUES(`sort_order`),
`enabled`=VALUES(`enabled`),`is_delete`=0;

-- ============================================================================
-- 初始化：业务配置
-- ============================================================================
INSERT INTO `system_config`
(`config_key`,`config_value`,`value_type`,`description`,`enabled`)
VALUES
('audio.max_upload_mb','50','INT','单个音频最大上传大小 MB；后续可根据服务器配置调整',1),
('audio.max_duration_sec','600','INT','单个音频最大时长（秒），默认 10 分钟',1),
('audio.normalized_sample_rate','44100','INT','FFmpeg 标准化采样率 Hz',1),
('audio.normalized_channels','1','INT','FFmpeg 标准化声道数；1=单声道',1),
('audio.temp_retention_hours','72','INT','未进入长期历史的临时音频默认保留小时数',1),
('audio.algorithm_version','cochlear-vocoder-v1','STRING','当前 Cochlear Vocoder 算法版本',1),
('audio.poll_interval_ms','1000','INT','小程序查询异步音频处理任务状态的建议轮询间隔',1),
('audio.storage_provider','LOCAL','STRING','V1 文件存储方式：LOCAL；后续可切 COS/OSS',1),
('audio.local_storage_root','/data/hear-like-me','STRING','Linux 生产环境建议的本地文件根目录；Windows 开发环境请覆盖配置',1),
('realtime.mic.mode','LOCAL_DSP','STRING','实时麦克风建议走小程序本地 DSP；最终以真机 PoC 结果为准',1),
('read_aloud.default_play_mode','ORIGINAL','STRING','点读默认直接播放后台配置音频；客户确认后可改 PROCESSED/BOTH',1),
('app.name','hear like me','STRING','项目名称',1),
('app.miniprogram_language','zh-CN','STRING','微信小程序默认语言',1),
('app.web_language','en-US','STRING','Web 默认语言',1)
ON DUPLICATE KEY UPDATE
`config_value`=VALUES(`config_value`),`value_type`=VALUES(`value_type`),
`description`=VALUES(`description`),`enabled`=VALUES(`enabled`);

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================================
-- 当前版本暂不建的表
-- ============================================================================
-- 1. read_aloud_hotspot：只有客户明确“一张图片多个点击区域”后再建。
-- 2. 独立 history 表：audio_processing_task + user_id 已可承担处理历史。
-- 3. 实时麦克风 session/chunk 表：若实时处理走小程序本地 DSP，无需落库。
-- 4. 支付/订单表：公益项目当前不需要。
-- ============================================================================
