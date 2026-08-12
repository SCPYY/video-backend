package com.project.module.admin.controller;

import com.project.common.response.Result;
import com.project.module.admin.dto.AdminLoginRequest;
import com.project.module.admin.dto.AdminLoginResponse;
import com.project.module.admin.service.AdminAuthService;
import com.project.module.admin.service.AdminMenuService;
import com.project.module.admin.dto.AdminMenuVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@Tag(name = "管理后台-认证", description = "管理员登录/登出")
@RestController
@RequestMapping("/api/v1/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;
    private final AdminMenuService adminMenuService;

    @Operation(summary="获取当前管理员菜单", description="根据当前管理员角色返回一级菜单、二级菜单和页面路由。角色取值：SUPER_ADMIN超管、ADMIN管理员、OPERATOR运营人员。")
    @GetMapping("/menus")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR','VIEWER')")
    public Result<List<AdminMenuVO>> menus() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String role = auth.getAuthorities().stream().findFirst().map(a -> a.getAuthority().replace("ROLE_", "")).orElse("OPERATOR");
        return Result.ok(adminMenuService.menus(role));
    }

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
