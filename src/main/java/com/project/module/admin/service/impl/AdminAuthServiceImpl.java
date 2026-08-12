package com.project.module.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.project.common.exception.BusinessException;
import com.project.common.exception.ErrorCode;
import com.project.infrastructure.security.JwtTokenProvider;
import com.project.module.admin.dto.AdminLoginRequest;
import com.project.module.admin.dto.AdminLoginResponse;
import com.project.module.admin.service.AdminAuthService;
import com.project.module.user.entity.SysAdmin;
import com.project.module.user.mapper.AdminMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {

    private final AdminMapper adminMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";

    @Override
    public AdminLoginResponse login(AdminLoginRequest request) {
        // 查询管理员
        SysAdmin admin = adminMapper.selectOne(new LambdaQueryWrapper<SysAdmin>()
                .eq(SysAdmin::getUsername, request.getUsername()));
        if (admin == null) {
            throw new BusinessException(ErrorCode.ADMIN_NOT_FOUND);
        }
        if (admin.getStatus() == 1) {
            throw new BusinessException(ErrorCode.ADMIN_FORBIDDEN);
        }
        if (!passwordEncoder.matches(request.getPassword(), admin.getPasswordHash())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }

        // 生成JWT
        // 新角色名对外使用，JWT 内部兼容现有接口注解：SUPER_ADMIN/ADMIN/OPERATOR -> ADMIN/EDITOR/VIEWER。
        String token = jwtTokenProvider.generateToken(admin.getId(), admin.getUsername(), legacyRole(admin.getRole()));

        // 更新最后登录时间
        admin.setLastLoginTime(LocalDateTime.now());
        adminMapper.updateById(admin);

        log.info("管理员登录成功: username={}, role={}", admin.getUsername(), admin.getRole());

        return AdminLoginResponse.builder()
                .accessToken(token)
                .adminId(admin.getId())
                .username(admin.getUsername())
                .role(admin.getRole())
                .build();
    }

    private String legacyRole(String role) {
        return switch (role == null ? "OPERATOR" : role.toUpperCase()) {
            case "SUPER_ADMIN" -> "ADMIN";
            case "ADMIN" -> "EDITOR";
            case "OPERATOR" -> "VIEWER";
            default -> role;
        };
    }

    @Override
    public void logout(Long adminId, String token) {
        // Token加入黑名单
        stringRedisTemplate.opsForValue().set(
                TOKEN_BLACKLIST_PREFIX + token,
                String.valueOf(adminId),
                jwtTokenProvider.getExpiration(),
                TimeUnit.MILLISECONDS);
        log.info("管理员登出: adminId={}", adminId);
    }
}
