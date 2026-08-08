package com.project.module.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.project.module.user.dto.*;
import com.project.module.user.entity.SysUser;

public interface UserService extends IService<SysUser> {

    /** 用户注册 */
    void register(RegisterRequest request);

    /** 用户登录 */
    LoginResponse login(LoginRequest request);

    /** 刷新Token */
    LoginResponse refreshToken(String refreshToken);

    /** 退出登录 */
    void logout(Long userId, String token);

    /** 获取个人信息 */
    UserProfileVO getProfile(Long userId);

    /** 更新个人信息 */
    void updateProfile(Long userId, UserProfileVO vo);
}
