package com.project.module.admin.controller;

import com.project.common.response.Result;
import com.project.module.admin.dto.AdminLoginRequest;
import com.project.module.admin.dto.AdminLoginResponse;
import com.project.module.admin.service.AdminAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理后台-认证", description = "管理员登录/登出")
@RestController
@RequestMapping("/api/v1/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @Operation(summary = "管理员登录")
    @PostMapping("/login")
    public Result<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        return Result.ok(adminAuthService.login(request));
    }

    @Operation(summary = "管理员登出")
    @PostMapping("/logout")
    public Result<Void> logout() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long adminId = (Long) auth.getPrincipal();
        String token = (String) auth.getCredentials();
        adminAuthService.logout(adminId, token);
        return Result.okMsg("已退出登录");
    }
}
