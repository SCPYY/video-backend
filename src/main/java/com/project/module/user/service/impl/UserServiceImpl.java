package com.project.module.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.project.common.exception.BusinessException;
import com.project.common.exception.ErrorCode;
import com.project.infrastructure.security.JwtTokenProvider;
import com.project.module.user.dto.*;
import com.project.module.user.entity.SysUser;
import com.project.module.user.mapper.UserMapper;
import com.project.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, SysUser> implements UserService {

    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";
    private static final String LOGIN_FAIL_PREFIX = "login:fail:";
    private static final int MAX_LOGIN_FAIL = 5;

    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        // 检查用户名是否已存在
        if (lambdaQuery().eq(SysUser::getUsername, request.getUsername()).count() > 0) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }
        // 检查邮箱是否已注册
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (lambdaQuery().eq(SysUser::getEmail, request.getEmail()).count() > 0) {
                throw new BusinessException(ErrorCode.EMAIL_EXISTS);
            }
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setNickname(request.getUsername());
        user.setStatus(0);
        baseMapper.insert(user);
        log.info("用户注册成功: id={}, username={}", user.getId(), user.getUsername());

        // 注册成功，自动签发Token
        return buildLoginResponse(user);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        // 检查登录失败次数
        String failKey = LOGIN_FAIL_PREFIX + request.getUsername();
        String failCountStr = stringRedisTemplate.opsForValue().get(failKey);
        if (failCountStr != null && Integer.parseInt(failCountStr) >= MAX_LOGIN_FAIL) {
            throw new BusinessException(ErrorCode.LOGIN_LOCKED);
        }

        SysUser user = lambdaQuery()
                .eq(SysUser::getUsername, request.getUsername())
                .one();

        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (user.getStatus() == 1) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            Long failCount = stringRedisTemplate.opsForValue().increment(failKey);
            stringRedisTemplate.expire(failKey, 30, TimeUnit.MINUTES);
            int remaining = MAX_LOGIN_FAIL - (failCount != null ? failCount.intValue() : 1);
            throw new BusinessException(ErrorCode.PASSWORD_ERROR.getCode(),
                    "密码错误，还剩" + Math.max(0, remaining) + "次尝试");
        }

        // 登录成功，清除失败记录
        stringRedisTemplate.delete(failKey);

        // 更新最后登录时间
        user.setLastLoginTime(LocalDateTime.now());
        baseMapper.updateById(user);

        log.info("用户登录成功: userId={}, username={}", user.getId(), user.getUsername());
        return buildLoginResponse(user);
    }

    @Override
    public LoginResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        Long userId = jwtTokenProvider.getUserId(refreshToken);
        SysUser user = getById(userId);
        if (user == null || user.getStatus() == 1) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        return buildLoginResponse(user);
    }

    @Override
    public void logout(Long userId, String token) {
        // 将Token加入黑名单，有效期与Token剩余有效期一致
        String blacklistKey = TOKEN_BLACKLIST_PREFIX + token;
        long ttl = jwtTokenProvider.getExpiration();
        stringRedisTemplate.opsForValue().set(blacklistKey, String.valueOf(userId), ttl, TimeUnit.MILLISECONDS);
        log.info("用户登出: userId={}", userId);
    }

    @Override
    public UserProfileVO getProfile(Long userId) {
        SysUser user = getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        UserProfileVO vo = new UserProfileVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }

    @Override
    @Transactional
    public void updateProfile(Long userId, UserProfileVO vo) {
        SysUser user = getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (vo.getNickname() != null) {
            user.setNickname(vo.getNickname());
        }
        if (vo.getAvatarUrl() != null) {
            user.setAvatarUrl(vo.getAvatarUrl());
        }
        if (vo.getPhone() != null) {
            user.setPhone(vo.getPhone());
        }
        baseMapper.updateById(user);
        log.info("用户信息更新: userId={}", userId);
    }

    private LoginResponse buildLoginResponse(SysUser user) {
        long expiresIn = jwtTokenProvider.getExpiration();
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), "USER");
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .email(user.getEmail())
                .build();

        return LoginResponse.builder()
                .accessToken(token)
                .refreshToken(refreshToken)
                .expiresIn(expiresIn)
                .tokenType("Bearer")
                .user(userInfo)
                .build();
    }
}
