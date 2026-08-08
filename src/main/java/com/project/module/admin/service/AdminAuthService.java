package com.project.module.admin.service;

import com.project.module.admin.dto.AdminLoginRequest;
import com.project.module.admin.dto.AdminLoginResponse;

public interface AdminAuthService {

    /**
     * 管理员登录
     */
    AdminLoginResponse login(AdminLoginRequest request);

    /**
     * 管理员登出
     */
    void logout(Long adminId, String token);
}
