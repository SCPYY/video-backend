package com.project.module.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.project.common.exception.BusinessException;
import com.project.common.exception.ErrorCode;
import com.project.common.response.Result;
import com.project.module.admin.dto.AdminAccountUpdateRequest;
import com.project.module.admin.dto.AdminAccountCreateRequest;
import com.project.module.admin.dto.AdminResetPasswordRequest;
import com.project.module.admin.service.AdminLogService;
import com.project.module.user.entity.SysAdmin;
import com.project.module.user.mapper.AdminMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理后台-管理员账号")
@RestController
@RequestMapping("/api/v1/admin/admins")
@RequiredArgsConstructor
public class AdminAccountController {
    private final AdminMapper adminMapper;
    private final PasswordEncoder passwordEncoder;
    private final AdminLogService adminLogService;

    @PostMapping
    @Operation(summary = "签发管理员账号", description = "超级管理员可签发SUPER_ADMIN、ADMIN、OPERATOR；管理员只能签发OPERATOR；运营人员无权签发账号。")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public Result<SysAdmin> create(@Valid @RequestBody AdminAccountCreateRequest request) {
        String role = request.getRole().toUpperCase();
        if (!validRole(role)) throw new BusinessException(ErrorCode.PARAM_ERROR, "角色无效");
        if (!isSuperAdmin() && !"OPERATOR".equals(role)) {
            throw new BusinessException(ErrorCode.ADMIN_FORBIDDEN, "管理员只能签发运营人员账号");
        }
        if (adminMapper.selectCount(new LambdaQueryWrapper<SysAdmin>().eq(SysAdmin::getUsername, request.getUsername())) > 0) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS, "管理员用户名已存在");
        }
        SysAdmin admin = new SysAdmin();
        admin.setUsername(request.getUsername().trim());
        admin.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        admin.setRole(role);
        admin.setStatus(0);
        adminMapper.insert(admin);
        admin.setPasswordHash(null);
        adminLogService.log(currentAdminId(), "CREATE", "ADMIN_ACCOUNT", String.valueOf(admin.getId()), null,
                java.util.Map.of("username", admin.getUsername(), "role", role));
        return Result.ok(admin);
    }

    @GetMapping
    @Operation(summary = "管理员账号列表", description = "超级管理员可查看全部账号；管理员仅可查看运营人员账号。")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public Result<Page<SysAdmin>> list(@RequestParam(required = false) String role,
                                       @RequestParam(required = false) Integer status,
                                       @RequestParam(defaultValue = "1") Integer page,
                                       @RequestParam(defaultValue = "20") Integer size) {
        String effectiveRole = isSuperAdmin() ? role : "OPERATOR";
        LambdaQueryWrapper<SysAdmin> w = new LambdaQueryWrapper<SysAdmin>()
                .eq(effectiveRole != null && !effectiveRole.isBlank(), SysAdmin::getRole, effectiveRole)
                .eq(status != null, SysAdmin::getStatus, status)
                .orderByDesc(SysAdmin::getId)
                .select(SysAdmin.class, f -> !"passwordHash".equals(f.getProperty()));
        return Result.ok(adminMapper.selectPage(new Page<>(Math.max(1, page), Math.min(100, Math.max(1, size))), w));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改管理员账号", description = "超级管理员可修改所有后台账号；管理员只能修改运营人员账号。角色和状态由超级管理员管理。")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody AdminAccountUpdateRequest request) {
        SysAdmin target = requireAdmin(id);
        checkTarget(target);
        if (!isSuperAdmin() && request.getRole() != null && !"OPERATOR".equals(request.getRole())) {
            throw new BusinessException(ErrorCode.ADMIN_FORBIDDEN, "管理员只能维护运营人员角色");
        }
        target.setUsername(request.getUsername());
        if (isSuperAdmin()) {
            if (request.getRole() != null && !validRole(request.getRole())) throw new BusinessException(ErrorCode.PARAM_ERROR, "角色无效");
            if (request.getRole() != null) target.setRole(request.getRole());
            if (request.getStatus() != null && (request.getStatus() < 0 || request.getStatus() > 1)) throw new BusinessException(ErrorCode.PARAM_ERROR, "状态无效");
            if (request.getStatus() != null) target.setStatus(request.getStatus());
        }
        adminMapper.updateById(target);
        adminLogService.log(currentAdminId(), "UPDATE", "ADMIN_ACCOUNT", String.valueOf(id), null, request);
        return Result.okMsg("管理员账号已更新");
    }

    @PutMapping("/{id}/password")
    @Operation(summary = "重置管理员密码", description = "超级管理员可重置所有后台账号密码；管理员只能重置运营人员密码。")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public Result<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody AdminResetPasswordRequest request) {
        SysAdmin target = requireAdmin(id);
        checkTarget(target);
        target.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        adminMapper.updateById(target);
        adminLogService.log(currentAdminId(), "RESET_PASSWORD", "ADMIN_ACCOUNT", String.valueOf(id), null, null);
        return Result.okMsg("管理员密码已重置");
    }

    private SysAdmin requireAdmin(Long id) { SysAdmin a = adminMapper.selectById(id); if (a == null) throw new BusinessException(ErrorCode.ADMIN_NOT_FOUND); return a; }
    private void checkTarget(SysAdmin target) { if (!isSuperAdmin() && !"OPERATOR".equals(target.getRole())) throw new BusinessException(ErrorCode.ADMIN_FORBIDDEN, "管理员只能操作运营人员"); }
    private boolean validRole(String role) { return "SUPER_ADMIN".equals(role) || "ADMIN".equals(role) || "OPERATOR".equals(role); }
    private boolean isSuperAdmin() { return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())); }
    private Long currentAdminId() { return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal(); }
}
