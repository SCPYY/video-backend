-- 为现有全部用户补充平台币钱包和基础展示数据，可重复执行
SET NAMES utf8mb4;
START TRANSACTION;

UPDATE sys_user
SET nickname = COALESCE(NULLIF(nickname, ''), CONCAT('测试用户', id)),
    avatar_url = COALESCE(NULLIF(avatar_url, ''), CONCAT('/uploads/avatar/test-', id, '.png')),
    phone = COALESCE(NULLIF(phone, ''), CONCAT('1390000', LPAD(id, 4, '0'))),
    updated_at = NOW();

INSERT INTO user_wallets (user_id, currency, available_balance, frozen_balance, status, version, created_at, updated_at)
SELECT id, 'PLATFORM_COIN', 100.00, 0.00, 1, 0, NOW(), NOW()
FROM sys_user
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO wallet_transactions
    (transaction_no, wallet_id, user_id, currency, type, direction, amount,
     balance_before, balance_after, related_type, related_id, idempotency_key, remark)
SELECT CONCAT('WLT-USER-SEED-', u.id), w.id, u.id, 'PLATFORM_COIN', 'ADJUSTMENT', 'IN',
       100.00, 0.00, 100.00, 'ADMIN', 'SYSTEM', CONCAT('ALL-USERS-SEED:', u.id), '全体用户平台币测试余额'
FROM sys_user u JOIN user_wallets w ON w.user_id = u.id AND w.currency = 'PLATFORM_COIN'
WHERE NOT EXISTS (SELECT 1 FROM wallet_transactions t WHERE t.idempotency_key = CONCAT('ALL-USERS-SEED:', u.id));

COMMIT;
