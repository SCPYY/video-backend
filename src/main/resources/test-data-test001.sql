-- test001 全模块联调测试数据，可重复执行
SET NAMES utf8mb4;
START TRANSACTION;
SET @test_user_id = (SELECT id FROM sys_user WHERE username = 'test001' LIMIT 1);
SET @admin_id = (SELECT id FROM sys_admin ORDER BY id LIMIT 1);

-- 为影游准备可购买商品
INSERT INTO products (type, content_id, episode_id, name, price_usd, price_eur, status)
SELECT 2, 3, NULL, '全集解锁-致命选择', 6.99, 6.49, 1
WHERE @test_user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM products WHERE type = 2 AND content_id = 3);

SET @game_product_id = (SELECT id FROM products WHERE type = 2 AND content_id = 3 ORDER BY id LIMIT 1);

-- 钱包：USD、EUR
-- 当前钱包已统一为平台币
INSERT INTO user_wallets (user_id, currency, available_balance, frozen_balance, status)
SELECT @test_user_id, 'PLATFORM_COIN', 150.00, 0.00, 1 WHERE @test_user_id IS NOT NULL
ON DUPLICATE KEY UPDATE available_balance = 150.00, frozen_balance = 0.00, status = 1;
SET @platform_wallet_id = (SELECT id FROM user_wallets WHERE user_id = @test_user_id AND currency = 'PLATFORM_COIN');
INSERT INTO wallet_transactions
    (transaction_no, wallet_id, user_id, currency, type, direction, amount, balance_before, balance_after,
     related_type, related_id, idempotency_key, remark)
SELECT 'WLT-TEST001-PLATFORM-SEED', @platform_wallet_id, @test_user_id, 'PLATFORM_COIN', 'RECHARGE', 'IN',
       150.00, 0.00, 150.00, 'ADMIN', CAST(@admin_id AS CHAR), 'TEST001:PLATFORM:SEED', 'test001 平台币测试余额'
WHERE @platform_wallet_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM wallet_transactions WHERE idempotency_key = 'TEST001:PLATFORM:SEED');

/* 历史 USD/EUR 测试钱包仅保留以下兼容代码时使用；统一平台币环境不再执行。 */
/*
INSERT INTO user_wallets (user_id, currency, available_balance, frozen_balance, status)
SELECT @test_user_id, 'USD', 100.00, 0.00, 1 WHERE @test_user_id IS NOT NULL
ON DUPLICATE KEY UPDATE available_balance = VALUES(available_balance), status = 1;
INSERT INTO user_wallets (user_id, currency, available_balance, frozen_balance, status)
SELECT @test_user_id, 'EUR', 50.00, 0.00, 1 WHERE @test_user_id IS NOT NULL
ON DUPLICATE KEY UPDATE available_balance = VALUES(available_balance), status = 1;

SET @usd_wallet_id = (SELECT id FROM user_wallets WHERE user_id = @test_user_id AND currency = 'USD');
SET @eur_wallet_id = (SELECT id FROM user_wallets WHERE user_id = @test_user_id AND currency = 'EUR');

INSERT INTO wallet_transactions
    (transaction_no, wallet_id, user_id, currency, type, direction, amount,
     balance_before, balance_after, related_type, related_id, idempotency_key, remark)
SELECT 'WLT-TEST001-USD-SEED', @usd_wallet_id, @test_user_id, 'USD', 'ADJUSTMENT', 'IN', 100.00,
       0.00, 100.00, 'ADMIN', CAST(@admin_id AS CHAR), 'TEST001:USD:SEED', 'test001 美元测试余额初始化'
WHERE @usd_wallet_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM wallet_transactions WHERE idempotency_key = 'TEST001:USD:SEED');

INSERT INTO wallet_transactions
    (transaction_no, wallet_id, user_id, currency, type, direction, amount,
     balance_before, balance_after, related_type, related_id, idempotency_key, remark)
SELECT 'WLT-TEST001-EUR-SEED', @eur_wallet_id, @test_user_id, 'EUR', 'ADJUSTMENT', 'IN', 50.00,
       0.00, 50.00, 'ADMIN', CAST(@admin_id AS CHAR), 'TEST001:EUR:SEED', 'test001 欧元测试余额初始化'
WHERE @eur_wallet_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM wallet_transactions WHERE idempotency_key = 'TEST001:EUR:SEED');
*/

-- 订单：已支付短剧、已支付影游、待支付、已取消、已退款
INSERT IGNORE INTO orders
    (order_no, user_id, product_id, amount, currency, payment_method, status,
     gateway_order_id, gateway_tx_id, paid_at, expired_at, created_at)
VALUES
    ('TEST001-DRAMA-PAID', @test_user_id, 3, 4.99, 'USD', 'WALLET', 1,
     'TEST-WALLET-DRAMA', 'TEST-TX-DRAMA', NOW() - INTERVAL 5 DAY, NOW() + INTERVAL 25 DAY, NOW() - INTERVAL 5 DAY),
    ('TEST001-GAME-PAID', @test_user_id, @game_product_id, 6.99, 'USD', 'PAYPAL', 1,
     'TEST-PAYPAL-GAME', 'TEST-TX-GAME', NOW() - INTERVAL 3 DAY, NOW() + INTERVAL 27 DAY, NOW() - INTERVAL 3 DAY),
    ('TEST001-EPISODE-PENDING', @test_user_id, 1, 0.99, 'USD', NULL, 0,
     NULL, NULL, NULL, NOW() + INTERVAL 30 MINUTE, NOW()),
    ('TEST001-DRAMA-CANCELLED', @test_user_id, 4, 0.99, 'USD', NULL, 2,
     NULL, NULL, NULL, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 2 DAY),
    ('TEST001-EPISODE-REFUNDED', @test_user_id, 2, 0.99, 'USD', 'STRIPE', 3,
     'TEST-STRIPE-REFUND', 'TEST-TX-REFUND', NOW() - INTERVAL 8 DAY, NOW() + INTERVAL 22 DAY, NOW() - INTERVAL 8 DAY);

-- 权益：短剧整部、短剧单集、影游整部、会员
INSERT INTO user_entitlements (user_id, type, content_id, episode_id, expire_time, created_at)
SELECT @test_user_id, 1, 1, NULL, NULL, NOW() - INTERVAL 5 DAY
WHERE @test_user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM user_entitlements WHERE user_id = @test_user_id AND type = 1 AND content_id = 1 AND episode_id IS NULL);

INSERT INTO user_entitlements (user_id, type, content_id, episode_id, expire_time, created_at)
SELECT @test_user_id, 1, 2, 5, NULL, NOW() - INTERVAL 4 DAY
WHERE @test_user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM user_entitlements WHERE user_id = @test_user_id AND type = 1 AND content_id = 2 AND episode_id = 5);

INSERT INTO user_entitlements (user_id, type, content_id, episode_id, expire_time, created_at)
SELECT @test_user_id, 1, 3, NULL, NULL, NOW() - INTERVAL 3 DAY
WHERE @test_user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM user_entitlements WHERE user_id = @test_user_id AND type = 1 AND content_id = 3 AND episode_id IS NULL);

INSERT INTO user_entitlements (user_id, type, content_id, episode_id, expire_time, created_at)
SELECT @test_user_id, 2, NULL, NULL, NOW() + INTERVAL 30 DAY, NOW()
WHERE @test_user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM user_entitlements WHERE user_id = @test_user_id AND type = 2 AND expire_time > NOW());

-- 评论、点赞、点踩、举报
INSERT INTO comments (content_id, episode_id, user_id, content, status, ip_address)
SELECT 1, 2, @test_user_id, 'test001：剧情节奏很好，这是评论模块联调数据。', 1, '127.0.0.1'
WHERE @test_user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM comments WHERE user_id = @test_user_id AND content = 'test001：剧情节奏很好，这是评论模块联调数据。');

SET @other_comment_id = (SELECT id FROM comments WHERE user_id <> @test_user_id AND status = 1 ORDER BY id LIMIT 1);
INSERT IGNORE INTO comment_likes (comment_id, user_id, type)
SELECT @other_comment_id, @test_user_id, 1 WHERE @other_comment_id IS NOT NULL;

SET @report_comment_id = (SELECT id FROM comments WHERE user_id <> @test_user_id AND status = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO comment_reports (comment_id, user_id, reason, status)
SELECT @report_comment_id, @test_user_id, '测试举报：用于后台审核流程联调', 0
WHERE @report_comment_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM comment_reports WHERE comment_id = @report_comment_id AND user_id = @test_user_id);

UPDATE comments c
SET c.like_count = (SELECT COUNT(*) FROM comment_likes cl WHERE cl.comment_id = c.id AND cl.type = 1),
    c.dislike_count = (SELECT COUNT(*) FROM comment_likes cl WHERE cl.comment_id = c.id AND cl.type = 2)
WHERE c.id = @other_comment_id;

-- 弹幕及弹幕点赞
INSERT INTO danmaku (episode_id, user_id, content, video_time, color, position, status)
SELECT 2, @test_user_id, 'test001：这里的反转太精彩了！', 18, '#FFCC00', 'scroll', 1
WHERE @test_user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM danmaku WHERE user_id = @test_user_id AND content = 'test001：这里的反转太精彩了！');

SET @other_danmaku_id = (SELECT id FROM danmaku WHERE user_id <> @test_user_id AND status = 1 ORDER BY id LIMIT 1);
INSERT IGNORE INTO danmaku_likes (danmaku_id, user_id)
SELECT @other_danmaku_id, @test_user_id WHERE @other_danmaku_id IS NOT NULL;

UPDATE danmaku d
SET d.like_count = (SELECT COUNT(*) FROM danmaku_likes dl WHERE dl.danmaku_id = d.id)
WHERE d.id = @other_danmaku_id;

-- 后台日志：记录本次测试数据初始化
INSERT INTO admin_logs (admin_id, action, module, target_id, after_data, ip_address, user_agent)
SELECT @admin_id, 'SEED_TEST_DATA', 'USER', CAST(@test_user_id AS CHAR),
       JSON_OBJECT('username', 'test001', 'modules', 'orders,entitlements,wallet,comments,danmaku'),
       '127.0.0.1', 'Codex test-data initializer'
WHERE @admin_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM admin_logs
      WHERE action = 'SEED_TEST_DATA' AND module = 'USER' AND target_id = CAST(@test_user_id AS CHAR)
  );

INSERT INTO user_security_logs (user_id, event_type, success, ip_address, user_agent, remark)
SELECT @test_user_id, 'LOGIN_SUCCESS', 1, '127.0.0.1', 'Codex test client', 'test001 登录成功测试记录'
WHERE @test_user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM user_security_logs WHERE user_id = @test_user_id AND event_type = 'LOGIN_SUCCESS' AND remark = 'test001 登录成功测试记录');
INSERT INTO user_security_logs (user_id, event_type, success, ip_address, user_agent, remark)
SELECT @test_user_id, 'LOGIN_FAILURE', 0, '127.0.0.1', 'Codex test client', 'test001 密码错误测试记录'
WHERE @test_user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM user_security_logs WHERE user_id = @test_user_id AND event_type = 'LOGIN_FAILURE' AND remark = 'test001 密码错误测试记录');

COMMIT;
