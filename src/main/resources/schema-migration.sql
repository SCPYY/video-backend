-- One-time migration for databases created by the original schema.sql.
-- MySQL 8.0.29+; run this against the existing application database.

SET @schema_name = DATABASE();
UPDATE sys_admin SET role='SUPER_ADMIN' WHERE username='admin' AND role='ADMIN';
CREATE TABLE IF NOT EXISTS system_messages (id BIGINT AUTO_INCREMENT PRIMARY KEY, recipient_type VARCHAR(16) NOT NULL, recipient_id BIGINT, message_type VARCHAR(32) NOT NULL, action_type VARCHAR(32), title VARCHAR(255) NOT NULL, content TEXT, target_type VARCHAR(32), target_id BIGINT, target_url VARCHAR(500), related_type VARCHAR(32), related_id BIGINT, is_read TINYINT NOT NULL DEFAULT 0, read_at DATETIME, created_at DATETIME DEFAULT CURRENT_TIMESTAMP, INDEX idx_message_recipient (recipient_type,recipient_id,created_at), INDEX idx_message_unread (recipient_type,recipient_id,is_read)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='system_messages' AND column_name='action_type')=0, 'ALTER TABLE system_messages ADD COLUMN action_type VARCHAR(32) AFTER message_type, ADD COLUMN target_type VARCHAR(32) AFTER content, ADD COLUMN target_id BIGINT AFTER target_type, ADD COLUMN target_url VARCHAR(500) AFTER target_id', 'SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='contents' AND column_name='content_status')=0, 'ALTER TABLE contents ADD COLUMN content_status TINYINT NOT NULL DEFAULT 0 AFTER status', 'SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='contents' AND column_name='reject_reason')=0, 'ALTER TABLE contents ADD COLUMN reject_reason VARCHAR(500) AFTER content_status', 'SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='contents' AND column_name='submitted_at')=0, 'ALTER TABLE contents ADD COLUMN submitted_at DATETIME, ADD COLUMN review_started_at DATETIME, ADD COLUMN reviewed_at DATETIME, ADD COLUMN published_at DATETIME, ADD COLUMN offline_at DATETIME, ADD COLUMN reviewed_by BIGINT, ADD COLUMN published_by BIGINT, ADD COLUMN offline_by BIGINT', 'SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
CREATE TABLE IF NOT EXISTS content_status_logs (id BIGINT AUTO_INCREMENT PRIMARY KEY, content_id BIGINT NOT NULL, from_status TINYINT, to_status TINYINT NOT NULL, action VARCHAR(32) NOT NULL, reason VARCHAR(500), operator_id BIGINT, ip_address VARCHAR(64), user_agent VARCHAR(500), created_at DATETIME DEFAULT CURRENT_TIMESTAMP, INDEX idx_content_status_log_content (content_id, created_at), INDEX idx_content_status_log_operator (operator_id, created_at)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='episodes' AND column_name='access_type')=0, 'ALTER TABLE episodes ADD COLUMN access_type TINYINT NOT NULL DEFAULT 1 AFTER is_free', 'SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='episodes' AND column_name='price_platform_coin')=0, 'ALTER TABLE episodes ADD COLUMN price_platform_coin DECIMAL(18,2) AFTER access_type', 'SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='products' AND column_name='price_platform_coin')=0, 'ALTER TABLE products ADD COLUMN price_platform_coin DECIMAL(18,2) AFTER price_eur', 'SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='contents' AND column_name='category_id')=0, 'ALTER TABLE contents ADD COLUMN category_id BIGINT AFTER category', 'SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=@schema_name AND table_name='contents' AND index_name='idx_content_category_id')=0, 'ALTER TABLE contents ADD INDEX idx_content_category_id (category_id)', 'SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS content_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY, type TINYINT NOT NULL, name VARCHAR(64) NOT NULL,
    description VARCHAR(255), icon_url VARCHAR(255), sort_order INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1, created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_category_type_name (type, name), INDEX idx_category_type_status (type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS interactive_scenes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY, content_id BIGINT NOT NULL, episode_id BIGINT, scene_no INT NOT NULL,
    title VARCHAR(255) NOT NULL, description TEXT, video_url VARCHAR(500), duration INT,
    scene_type VARCHAR(16) NOT NULL DEFAULT 'NORMAL', is_start TINYINT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 0, view_count BIGINT NOT NULL DEFAULT 0, play_count BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_scene_no (content_id, scene_no), INDEX idx_scene_content (content_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS interactive_nodes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY, scene_id BIGINT NOT NULL, node_no INT NOT NULL, prompt VARCHAR(500) NOT NULL,
    node_type VARCHAR(16) NOT NULL DEFAULT 'SINGLE', show_at INT, timeout_seconds INT, required TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_node_no (scene_id, node_no), INDEX idx_node_scene (scene_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS interactive_options (
    id BIGINT AUTO_INCREMENT PRIMARY KEY, node_id BIGINT NOT NULL, option_no INT NOT NULL, title VARCHAR(255) NOT NULL,
    description VARCHAR(500), next_scene_id BIGINT, next_node_id BIGINT, condition_config JSON, effect_config JSON,
    status TINYINT NOT NULL DEFAULT 1, created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_option_no (node_id, option_no), INDEX idx_option_node (node_id), INDEX idx_option_next_scene (next_scene_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='episodes' AND column_name='is_preview')=0, 'ALTER TABLE episodes ADD COLUMN is_preview TINYINT NOT NULL DEFAULT 0 AFTER is_free', 'SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='episodes' AND column_name='view_count')=0, 'ALTER TABLE episodes ADD COLUMN view_count BIGINT NOT NULL DEFAULT 0 AFTER sort_order', 'SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='episodes' AND column_name='play_count')=0, 'ALTER TABLE episodes ADD COLUMN play_count BIGINT NOT NULL DEFAULT 0 AFTER view_count', 'SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='episodes' AND column_name='status')=0, 'ALTER TABLE episodes ADD COLUMN status TINYINT NOT NULL DEFAULT 0 AFTER play_count', 'SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='episodes' AND column_name='updated_at')=0, 'ALTER TABLE episodes ADD COLUMN updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at', 'SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='contents' AND column_name='play_count')=0, 'ALTER TABLE contents ADD COLUMN play_count BIGINT NOT NULL DEFAULT 0 AFTER view_count', 'SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='contents' AND column_name='unique_view_count')=0, 'ALTER TABLE contents ADD COLUMN unique_view_count BIGINT NOT NULL DEFAULT 0 AFTER play_count', 'SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='contents' AND column_name='play_user_count')=0, 'ALTER TABLE contents ADD COLUMN play_user_count BIGINT NOT NULL DEFAULT 0 AFTER unique_view_count', 'SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='contents' AND column_name='like_count')=0, 'ALTER TABLE contents ADD COLUMN like_count BIGINT NOT NULL DEFAULT 0 AFTER unique_view_count', 'SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='contents' AND column_name='favorite_count')=0, 'ALTER TABLE contents ADD COLUMN favorite_count BIGINT NOT NULL DEFAULT 0 AFTER like_count', 'SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='contents' AND column_name='share_count')=0, 'ALTER TABLE contents ADD COLUMN share_count BIGINT NOT NULL DEFAULT 0 AFTER favorite_count', 'SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='contents' AND column_name='comment_count')=0, 'ALTER TABLE contents ADD COLUMN comment_count BIGINT NOT NULL DEFAULT 0 AFTER share_count', 'SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='contents' AND column_name='danmaku_count')=0, 'ALTER TABLE contents ADD COLUMN danmaku_count BIGINT NOT NULL DEFAULT 0 AFTER comment_count', 'SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS user_security_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    success TINYINT NOT NULL DEFAULT 1,
    ip_address VARCHAR(64),
    user_agent VARCHAR(500),
    remark VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_security_user_created (user_id, created_at),
    INDEX idx_security_event (event_type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE user_wallets MODIFY currency VARCHAR(32) NOT NULL;
ALTER TABLE wallet_transactions MODIFY currency VARCHAR(32) NOT NULL;

-- 合并历史多币种钱包为每个用户唯一的平台币钱包。
INSERT INTO user_wallets (user_id, currency, available_balance, frozen_balance, status, version, created_at, updated_at)
SELECT user_id, 'PLATFORM_COIN', SUM(available_balance), SUM(frozen_balance),
       CASE WHEN SUM(status = 3) > 0 AND SUM(available_balance) = 0 AND SUM(frozen_balance) = 0 THEN 3
            WHEN SUM(status = 2) > 0 THEN 2 ELSE 1 END, 0, MIN(created_at), NOW()
FROM user_wallets WHERE currency <> 'PLATFORM_COIN'
GROUP BY user_id
ON DUPLICATE KEY UPDATE available_balance = available_balance + VALUES(available_balance),
    frozen_balance = frozen_balance + VALUES(frozen_balance), updated_at = NOW();
UPDATE wallet_transactions SET currency = 'PLATFORM_COIN' WHERE currency <> 'PLATFORM_COIN';
DELETE FROM user_wallets WHERE currency <> 'PLATFORM_COIN';
ALTER TABLE user_wallets MODIFY currency VARCHAR(32) NOT NULL COMMENT '统一平台币：PLATFORM_COIN';
ALTER TABLE wallet_transactions MODIFY currency VARCHAR(32) NOT NULL COMMENT '统一平台币：PLATFORM_COIN';

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='user_wallets' AND column_name='status_reason')=0,
    'ALTER TABLE user_wallets ADD COLUMN status_reason VARCHAR(255)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='user_wallets' AND column_name='disabled_at')=0,
    'ALTER TABLE user_wallets ADD COLUMN disabled_at DATETIME', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='user_wallets' AND column_name='disabled_by')=0,
    'ALTER TABLE user_wallets ADD COLUMN disabled_by BIGINT', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='sys_user' AND column_name='status_reason')=0,
    'ALTER TABLE sys_user ADD COLUMN status_reason VARCHAR(255)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='sys_user' AND column_name='disabled_at')=0,
    'ALTER TABLE sys_user ADD COLUMN disabled_at DATETIME', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='sys_user' AND column_name='disabled_by')=0,
    'ALTER TABLE sys_user ADD COLUMN disabled_by BIGINT', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='sys_user' AND column_name='token_version')=0,
    'ALTER TABLE sys_user ADD COLUMN token_version INT NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = @schema_name AND table_name = 'contents' AND column_name = 'admin_remark') = 0,
    'ALTER TABLE contents ADD COLUMN admin_remark VARCHAR(500)',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = @schema_name AND table_name = 'contents' AND column_name = 'created_by') = 0,
    'ALTER TABLE contents ADD COLUMN created_by BIGINT',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = @schema_name AND table_name = 'contents' AND column_name = 'updated_by') = 0,
    'ALTER TABLE contents ADD COLUMN updated_by BIGINT',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = @schema_name AND table_name = 'episodes' AND column_name = 'source_type') = 0,
    'ALTER TABLE episodes ADD COLUMN source_type TINYINT DEFAULT 1',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = @schema_name AND table_name = 'episodes' AND column_name = 'transcode_status') = 0,
    'ALTER TABLE episodes ADD COLUMN transcode_status TINYINT DEFAULT 0',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = @schema_name AND table_name = 'episodes' AND column_name = 'original_filename') = 0,
    'ALTER TABLE episodes ADD COLUMN original_filename VARCHAR(255)',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = @schema_name AND table_name = 'episodes' AND column_name = 'file_size') = 0,
    'ALTER TABLE episodes ADD COLUMN file_size BIGINT',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content_id BIGINT,
    episode_id BIGINT,
    user_id BIGINT NOT NULL,
    parent_id BIGINT,
    root_id BIGINT,
    reply_to_user_id BIGINT,
    content VARCHAR(1000) NOT NULL,
    like_count INT NOT NULL DEFAULT 0,
    dislike_count INT NOT NULL DEFAULT 0,
    reply_count INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    ip_address VARCHAR(64),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_comment_content (content_id, status, created_at),
    INDEX idx_comment_episode (episode_id, status, created_at),
    INDEX idx_comment_parent (parent_id),
    INDEX idx_comment_root (root_id),
    INDEX idx_comment_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS comment_likes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    comment_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    type TINYINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_comment_user (comment_id, user_id),
    INDEX idx_comment_like_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS danmaku (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    episode_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content VARCHAR(200) NOT NULL,
    video_time INT NOT NULL,
    color VARCHAR(16) NOT NULL DEFAULT '#FFFFFF',
    position VARCHAR(16) NOT NULL DEFAULT 'scroll',
    like_count INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_danmaku_episode_time (episode_id, video_time, status),
    INDEX idx_danmaku_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS danmaku_likes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    danmaku_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_danmaku_user (danmaku_id, user_id),
    INDEX idx_danmaku_like_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS comment_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    comment_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    reason VARCHAR(255),
    status TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_report_comment (comment_id),
    INDEX idx_report_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS admin_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    admin_id BIGINT NOT NULL,
    action VARCHAR(32) NOT NULL,
    module VARCHAR(32) NOT NULL,
    target_id VARCHAR(64),
    before_data JSON,
    after_data JSON,
    ip_address VARCHAR(64),
    user_agent VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_admin_log_admin (admin_id),
    INDEX idx_admin_log_module (module),
    INDEX idx_admin_log_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS search_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    keyword VARCHAR(100) NOT NULL,
    normalized_keyword VARCHAR(100) NOT NULL,
    result_count INT NOT NULL DEFAULT 0,
    ip_address VARCHAR(64),
    user_agent VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_search_keyword_time (normalized_keyword, created_at),
    INDEX idx_search_created (created_at),
    INDEX idx_search_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_wallets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    currency VARCHAR(8) NOT NULL,
    available_balance DECIMAL(18, 2) NOT NULL DEFAULT 0.00,
    frozen_balance DECIMAL(18, 2) NOT NULL DEFAULT 0.00,
    status TINYINT NOT NULL DEFAULT 1,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_wallet_user_currency (user_id, currency),
    INDEX idx_wallet_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS wallet_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_no VARCHAR(64) NOT NULL,
    wallet_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    currency VARCHAR(8) NOT NULL,
    type VARCHAR(32) NOT NULL,
    direction VARCHAR(8) NOT NULL,
    amount DECIMAL(18, 2) NOT NULL,
    balance_before DECIMAL(18, 2) NOT NULL,
    balance_after DECIMAL(18, 2) NOT NULL,
    related_type VARCHAR(32),
    related_id VARCHAR(64),
    idempotency_key VARCHAR(128) NOT NULL,
    remark VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_wallet_transaction_no (transaction_no),
    UNIQUE KEY uk_wallet_idempotency_key (idempotency_key),
    INDEX idx_wallet_transaction_wallet_created (wallet_id, created_at),
    INDEX idx_wallet_transaction_user_created (user_id, created_at),
    INDEX idx_wallet_transaction_related (related_type, related_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
