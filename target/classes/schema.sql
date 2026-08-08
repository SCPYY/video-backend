-- ============================================
-- 海外短剧+影游独立站 数据库初始化脚本
-- 数据库: MySQL 8.0 / H2 (开发环境)
-- ============================================

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    email VARCHAR(128),
    phone VARCHAR(32),
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(64),
    avatar_url VARCHAR(255),
    status TINYINT DEFAULT 0 COMMENT '0-正常 1-禁用',
    last_login_time DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_email (email)
);

-- 管理员表
CREATE TABLE IF NOT EXISTS sys_admin (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(32) DEFAULT 'EDITOR' COMMENT 'ADMIN/EDITOR/VIEWER',
    status TINYINT DEFAULT 0 COMMENT '0-正常 1-禁用',
    last_login_time DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 内容主表
CREATE TABLE IF NOT EXISTS contents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type TINYINT NOT NULL COMMENT '1-短剧 2-影游',
    title VARCHAR(255) NOT NULL,
    description TEXT,
    cover_url VARCHAR(255),
    category VARCHAR(64),
    tags VARCHAR(255) COMMENT '逗号分隔',
    status TINYINT DEFAULT 0 COMMENT '0-下架 1-上架',
    view_count BIGINT DEFAULT 0,
    sort_order INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_type (type),
    INDEX idx_category (category)
);

-- 内容扩展表 (EAV模式)
CREATE TABLE IF NOT EXISTS content_extras (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content_id BIGINT NOT NULL,
    `key` VARCHAR(64) NOT NULL,
    `value` JSON,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_extras_content FOREIGN KEY (content_id) REFERENCES contents(id) ON DELETE CASCADE,
    UNIQUE INDEX uk_content_key (content_id, `key`)
);

-- 剧集/关卡表
CREATE TABLE IF NOT EXISTS episodes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content_id BIGINT NOT NULL,
    episode_number INT NOT NULL,
    title VARCHAR(255),
    video_url VARCHAR(255),
    duration INT COMMENT '时长(秒)',
    interactive_config JSON COMMENT '影游互动配置',
    is_free TINYINT DEFAULT 0 COMMENT '0-付费 1-免费',
    sort_order INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_episodes_content FOREIGN KEY (content_id) REFERENCES contents(id) ON DELETE CASCADE,
    INDEX idx_content_id (content_id)
);

-- 商品表
CREATE TABLE IF NOT EXISTS products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type TINYINT NOT NULL COMMENT '1-单集解锁 2-全集解锁 3-会员',
    content_id BIGINT,
    episode_id BIGINT,
    name VARCHAR(128) NOT NULL,
    price_usd DECIMAL(10,2),
    price_eur DECIMAL(10,2),
    duration_days INT COMMENT '会员有效天数',
    status TINYINT DEFAULT 1 COMMENT '0-下架 1-上架',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_content_id (content_id),
    INDEX idx_status (status)
);

-- 订单表
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(8) NOT NULL COMMENT 'USD/EUR',
    payment_method VARCHAR(32) COMMENT 'PAYPAL/STRIPE',
    status TINYINT DEFAULT 0 COMMENT '0-待支付 1-已支付 2-已取消 3-已退款',
    gateway_order_id VARCHAR(128),
    gateway_tx_id VARCHAR(128),
    paid_at DATETIME,
    expired_at DATETIME COMMENT '订单过期时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_order_no (order_no),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_gateway_order_id (gateway_order_id)
);

-- 用户权益表
CREATE TABLE IF NOT EXISTS user_entitlements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type TINYINT NOT NULL COMMENT '1-内容解锁 2-会员',
    content_id BIGINT,
    episode_id BIGINT,
    expire_time DATETIME COMMENT 'NULL表示永久',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_content_id (content_id),
    INDEX idx_expire_time (expire_time)
);

-- ============================================
-- 初始种子数据
-- ============================================

-- 默认管理员 (密码: admin123, BCrypt加密)
-- 实际使用时请更换密码
INSERT INTO sys_admin (username, password_hash, role, status) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'ADMIN', 0);
