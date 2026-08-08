package com.project.module.user.controller;

import com.project.common.response.Result;
import com.project.module.user.dto.LoginRequest;
import com.project.module.user.dto.LoginResponse;
import com.project.module.user.dto.RegisterRequest;
import com.project.module.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证接口", description = "用户注册、登录、Token管理")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request);
        return Result.okMsg("注册成功");
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(userService.login(request));
    }

    @Operation(summary = "刷新Token", description = "使用refreshToken获取新的accessToken")
    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(
            @Parameter(description = "刷新令牌", required = true) @RequestParam String refreshToken) {
        return Result.ok(userService.refreshToken(refreshToken));
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) auth.getPrincipal();
        String token = (String) auth.getCredentials();
        userService.logout(userId, token);
        return Result.okMsg("已退出登录");
    }
}
