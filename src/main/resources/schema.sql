-- 海外短剧 + 影游独立站数据库初始化脚本
-- MySQL 8.0

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    email VARCHAR(128),
    phone VARCHAR(32),
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(64),
    avatar_url VARCHAR(255),
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0-正常 1-禁用 2-注销',
    status_reason VARCHAR(255),
    disabled_at DATETIME,
    disabled_by BIGINT,
    token_version INT NOT NULL DEFAULT 0,
    last_login_time DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_email (email),
    INDEX idx_user_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_admin (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL DEFAULT 'OPERATOR' COMMENT 'SUPER_ADMIN/ADMIN/OPERATOR',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0-正常 1-禁用',
    last_login_time DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS contents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type TINYINT NOT NULL COMMENT '1-短剧 2-影游',
    title VARCHAR(255) NOT NULL,
    description TEXT,
    cover_url VARCHAR(255),
    category VARCHAR(64),
    category_id BIGINT,
    tags VARCHAR(255),
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0-下架 1-上架',
    content_status TINYINT NOT NULL DEFAULT 0 COMMENT '0草稿 1待审核 2审核中 3待上架 4上架 5下架 6删除 7驳回',
    reject_reason VARCHAR(500),
    submitted_at DATETIME, review_started_at DATETIME, reviewed_at DATETIME, published_at DATETIME, offline_at DATETIME,
    reviewed_by BIGINT, published_by BIGINT, offline_by BIGINT,
    view_count BIGINT NOT NULL DEFAULT 0,
    play_count BIGINT NOT NULL DEFAULT 0,
    unique_view_count BIGINT NOT NULL DEFAULT 0,
    play_user_count BIGINT NOT NULL DEFAULT 0,
    like_count BIGINT NOT NULL DEFAULT 0,
    favorite_count BIGINT NOT NULL DEFAULT 0,
    share_count BIGINT NOT NULL DEFAULT 0,
    comment_count BIGINT NOT NULL DEFAULT 0,
    danmaku_count BIGINT NOT NULL DEFAULT 0,
    sort_order INT NOT NULL DEFAULT 0,
    admin_remark VARCHAR(500),
    created_by BIGINT,
    updated_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_content_status (status),
    INDEX idx_content_type (type),
    INDEX idx_content_category (category),
    INDEX idx_content_category_id (category_id),
    INDEX idx_content_hot (status, type, view_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS content_status_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY, content_id BIGINT NOT NULL, from_status TINYINT, to_status TINYINT NOT NULL,
    action VARCHAR(32) NOT NULL, reason VARCHAR(500), operator_id BIGINT, ip_address VARCHAR(64), user_agent VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP, INDEX idx_content_status_log_content (content_id, created_at), INDEX idx_content_status_log_operator (operator_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS system_messages (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, recipient_type VARCHAR(16) NOT NULL, recipient_id BIGINT,
 message_type VARCHAR(32) NOT NULL, action_type VARCHAR(32), title VARCHAR(255) NOT NULL, content TEXT,
 target_type VARCHAR(32), target_id BIGINT, target_url VARCHAR(500),
 related_type VARCHAR(32), related_id BIGINT, is_read TINYINT NOT NULL DEFAULT 0, read_at DATETIME,
 created_at DATETIME DEFAULT CURRENT_TIMESTAMP, INDEX idx_message_recipient (recipient_type,recipient_id,created_at), INDEX idx_message_unread (recipient_type,recipient_id,is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS content_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type TINYINT NOT NULL COMMENT '1-短剧 2-影游',
    name VARCHAR(64) NOT NULL,
    description VARCHAR(255),
    icon_url VARCHAR(255),
    sort_order INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0-禁用 1-启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_category_type_name (type, name),
    INDEX idx_category_type_status (type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS content_extras (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content_id BIGINT NOT NULL,
    `key` VARCHAR(64) NOT NULL,
    `value` JSON,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_extras_content FOREIGN KEY (content_id) REFERENCES contents(id) ON DELETE CASCADE,
    UNIQUE KEY uk_content_key (content_id, `key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS episodes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content_id BIGINT NOT NULL,
    episode_number INT NOT NULL,
    title VARCHAR(255),
    video_url VARCHAR(255),
    duration INT COMMENT '时长（秒）',
    interactive_config JSON,
    is_free TINYINT NOT NULL DEFAULT 0 COMMENT '0-付费 1-免费',
    access_type TINYINT NOT NULL DEFAULT 1 COMMENT '1-免费集 2-付费集 3-会员免费',
    price_platform_coin DECIMAL(18,2),
    is_preview TINYINT NOT NULL DEFAULT 0 COMMENT '0-不可试看 1-可试看',
    sort_order INT NOT NULL DEFAULT 0,
    view_count BIGINT NOT NULL DEFAULT 0,
    play_count BIGINT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0-草稿 1-上架 2-下架',
    source_type TINYINT DEFAULT 1 COMMENT '1-本地上传 2-第三方URL',
    transcode_status TINYINT DEFAULT 0 COMMENT '0-未转码 1-转码中 2-已完成 3-失败',
    original_filename VARCHAR(255),
    file_size BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_episodes_content FOREIGN KEY (content_id) REFERENCES contents(id) ON DELETE CASCADE,
    UNIQUE KEY uk_episode_number (content_id, episode_number),
    INDEX idx_episode_content_sort (content_id, sort_order, episode_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS interactive_scenes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY, content_id BIGINT NOT NULL, episode_id BIGINT,
    scene_no INT NOT NULL, title VARCHAR(255) NOT NULL, description TEXT, video_url VARCHAR(500),
    duration INT, scene_type VARCHAR(16) NOT NULL DEFAULT 'NORMAL', is_start TINYINT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 0, view_count BIGINT NOT NULL DEFAULT 0, play_count BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_scene_no (content_id, scene_no), INDEX idx_scene_content (content_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS interactive_nodes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY, scene_id BIGINT NOT NULL, node_no INT NOT NULL,
    prompt VARCHAR(500) NOT NULL, node_type VARCHAR(16) NOT NULL DEFAULT 'SINGLE', show_at INT,
    timeout_seconds INT, required TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_node_no (scene_id, node_no), INDEX idx_node_scene (scene_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS interactive_options (
    id BIGINT AUTO_INCREMENT PRIMARY KEY, node_id BIGINT NOT NULL, option_no INT NOT NULL,
    title VARCHAR(255) NOT NULL, description VARCHAR(500), next_scene_id BIGINT, next_node_id BIGINT,
    condition_config JSON, effect_config JSON, status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_option_no (node_id, option_no), INDEX idx_option_node (node_id), INDEX idx_option_next_scene (next_scene_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS interactive_progress (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL, content_id BIGINT NOT NULL, current_scene_id BIGINT, current_node_id BIGINT,
    last_option_id BIGINT, progress_seconds INT NOT NULL DEFAULT 0, is_finished TINYINT NOT NULL DEFAULT 0,
    ending_scene_id BIGINT, last_played_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_interactive_progress_user_content (user_id, content_id),
    INDEX idx_interactive_progress_content (content_id, last_played_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS interactive_choices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL, content_id BIGINT NOT NULL, scene_id BIGINT NOT NULL, node_id BIGINT NOT NULL,
    option_id BIGINT NOT NULL, next_scene_id BIGINT, next_node_id BIGINT, video_position INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_interactive_choice_user_content (user_id, content_id, created_at),
    INDEX idx_interactive_choice_option (option_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type TINYINT NOT NULL COMMENT '1-单集解锁 2-全集解锁 3-会员',
    content_id BIGINT,
    episode_id BIGINT,
    name VARCHAR(128) NOT NULL,
    price_usd DECIMAL(10,2),
    price_eur DECIMAL(10,2),
    price_platform_coin DECIMAL(18,2),
    duration_days INT,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0-下架 1-上架',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_product_content (content_id),
    INDEX idx_product_episode (episode_id),
    INDEX idx_product_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(8) NOT NULL COMMENT 'USD/EUR',
    payment_method VARCHAR(32) COMMENT 'PAYPAL/STRIPE/WALLET',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0-待支付 1-已支付 2-已取消 3-已退款',
    gateway_order_id VARCHAR(128),
    gateway_tx_id VARCHAR(128),
    paid_at DATETIME,
    expired_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_order_user (user_id),
    INDEX idx_order_product (product_id),
    INDEX idx_order_status (status),
    INDEX idx_order_gateway (gateway_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_entitlements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type TINYINT NOT NULL COMMENT '1-内容解锁 2-会员',
    content_id BIGINT,
    episode_id BIGINT,
    expire_time DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_entitlement_user (user_id),
    INDEX idx_entitlement_content (content_id),
    INDEX idx_entitlement_episode (episode_id),
    INDEX idx_entitlement_expire (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1-正常 2-已删除 3-审核中',
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
    type TINYINT NOT NULL COMMENT '1-点赞 2-点踩',
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
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1-正常 2-已删除',
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
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0-待处理 1-已处理 2-驳回',
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

CREATE TABLE IF NOT EXISTS user_security_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    event_type VARCHAR(32) NOT NULL COMMENT 'LOGIN_SUCCESS/LOGIN_FAILURE/LOGOUT/PASSWORD_RESET',
    success TINYINT NOT NULL DEFAULT 1,
    ip_address VARCHAR(64),
    user_agent VARCHAR(500),
    remark VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_security_user_created (user_id, created_at),
    INDEX idx_security_event (event_type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 默认管理员：admin / admin123（生产环境必须立即修改）
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
    currency VARCHAR(32) NOT NULL COMMENT '统一平台币：PLATFORM_COIN',
    available_balance DECIMAL(18, 2) NOT NULL DEFAULT 0.00,
    frozen_balance DECIMAL(18, 2) NOT NULL DEFAULT 0.00,
    status TINYINT NOT NULL DEFAULT 1,
    status_reason VARCHAR(255),
    disabled_at DATETIME,
    disabled_by BIGINT,
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
    currency VARCHAR(32) NOT NULL COMMENT '统一平台币：PLATFORM_COIN',
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

INSERT IGNORE INTO sys_admin (username, password_hash, role, status)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'SUPER_ADMIN', 0);
