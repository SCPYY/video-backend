-- 消息中心测试数据，可重复执行
SET NAMES utf8mb4;
START TRANSACTION;

SET @test_user_id = (SELECT id FROM sys_user WHERE username = 'test001' LIMIT 1);

INSERT INTO system_messages
    (recipient_type, recipient_id, message_type, action_type, title, content, target_type, target_id, target_url, related_type, related_id, is_read)
SELECT 'USER', @test_user_id, 'ORDER', 'VIEW_ORDER', '订单支付成功', '你的测试订单已支付成功，平台币已扣除。', 'ORDER', 1, '/orders/1', 'ORDER', 1, 0
WHERE @test_user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_messages WHERE recipient_type='USER' AND recipient_id=@test_user_id AND message_type='ORDER' AND title='订单支付成功');

INSERT INTO system_messages
    (recipient_type, recipient_id, message_type, action_type, title, content, target_type, target_id, target_url, related_type, related_id, is_read)
SELECT 'USER', @test_user_id, 'WALLET', 'VIEW_WALLET', '平台币到账提醒', '测试平台币已到账 100.00，可用于购买内容。', 'WALLET', @test_user_id, '/wallet', 'WALLET', @test_user_id, 0
WHERE @test_user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_messages WHERE recipient_type='USER' AND recipient_id=@test_user_id AND message_type='WALLET' AND title='平台币到账提醒');

INSERT INTO system_messages
    (recipient_type, recipient_id, message_type, title, content, related_type, related_id, is_read)
SELECT 'USER', @test_user_id, 'ENTITLEMENT', '权益已到账', '你已获得测试短剧和影游权益，可进入我的权益查看。', 'USER_ENTITLEMENT', @test_user_id, 1
WHERE @test_user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_messages WHERE recipient_type='USER' AND recipient_id=@test_user_id AND message_type='ENTITLEMENT' AND title='权益已到账');

INSERT INTO system_messages
    (recipient_type, recipient_id, message_type, title, content, related_type, related_id, is_read)
SELECT 'USER', @test_user_id, 'SECURITY', '登录安全提醒', '检测到一次测试登录成功记录，如非本人操作请及时修改密码。', 'USER', @test_user_id, 1
WHERE @test_user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_messages WHERE recipient_type='USER' AND recipient_id=@test_user_id AND message_type='SECURITY' AND title='登录安全提醒');

INSERT INTO system_messages
    (recipient_type, recipient_id, message_type, title, content, related_type, related_id, is_read)
SELECT 'ADMIN', NULL, 'CONTENT_REVIEW', '待审核内容提醒', '测试内容已提交审核，请管理员进入审核工作台处理。', 'CONTENT', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM system_messages WHERE recipient_type='ADMIN' AND recipient_id IS NULL AND message_type='CONTENT_REVIEW' AND title='待审核内容提醒');

INSERT INTO system_messages
    (recipient_type, recipient_id, message_type, title, content, related_type, related_id, is_read)
SELECT 'ADMIN', 1, 'SYSTEM', '系统测试通知', '这是管理端消息中心的系统通知测试数据。', 'SYSTEM', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM system_messages WHERE recipient_type='ADMIN' AND recipient_id=1 AND message_type='SYSTEM' AND title='系统测试通知');

COMMIT;
