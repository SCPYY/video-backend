-- 管理端三角色测试账号；密码沿用 schema.sql 中 admin 测试账号密码
SET @admin_password_hash = '$2a$10$bJhg1RIMaUpqQIVy7GAmPOecelQE2OiBQAnhQwxr0QEWkoaWlxT/2';
INSERT INTO sys_admin (username, password_hash, role, status)
VALUES ('superadmin_test', @admin_password_hash, 'SUPER_ADMIN', 0)
ON DUPLICATE KEY UPDATE password_hash=@admin_password_hash, role='SUPER_ADMIN', status=0;
INSERT INTO sys_admin (username, password_hash, role, status)
VALUES ('admin_test', @admin_password_hash, 'ADMIN', 0)
ON DUPLICATE KEY UPDATE password_hash=@admin_password_hash, role='ADMIN', status=0;
INSERT INTO sys_admin (username, password_hash, role, status)
VALUES ('operator_test', @admin_password_hash, 'OPERATOR', 0)
ON DUPLICATE KEY UPDATE password_hash=@admin_password_hash, role='OPERATOR', status=0;
