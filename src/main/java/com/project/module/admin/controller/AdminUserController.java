package com.project.module.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.project.common.response.Result;
import com.project.common.exception.BusinessException;
import com.project.common.exception.ErrorCode;
import com.project.module.admin.dto.AdminResetPasswordRequest;
import com.project.module.admin.dto.UserStatusRequest;
import com.project.module.admin.dto.UserUpdateRequest;
import com.project.module.admin.service.AdminLogService;
import com.project.module.user.entity.SysUser;
import com.project.module.user.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.LocalDateTime;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理后台-用户管理", description = "用户列表、禁用/启用管理")
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AdminLogService adminLogService;

    @Operation(summary = "用户列表")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR','VIEWER')")
    public Result<Page<SysUser>> list(
            @Parameter(description = "状态：0-正常，1-禁用，2-注销") @RequestParam(required = false) Integer status,
            @Parameter(description = "关键词（用户名/邮箱模糊搜索）") @RequestParam(required = false) String keyword,
            @Parameter(description = "昵称模糊搜索") @RequestParam(required = false) String nickname,
            @Parameter(description = "手机号精确搜索") @RequestParam(required = false) String phone,
            @Parameter(description = "注册时间起，ISO 格式") @RequestParam(required = false) LocalDateTime createdFrom,
            @Parameter(description = "注册时间止，ISO 格式") @RequestParam(required = false) LocalDateTime createdTo,
            @Parameter(description = "最近登录时间起，ISO 格式") @RequestParam(required = false) LocalDateTime lastLoginFrom,
            @Parameter(description = "最近登录时间止，ISO 格式") @RequestParam(required = false) LocalDateTime lastLoginTo,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") Integer size) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(status != null, SysUser::getStatus, status);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getEmail, keyword)
                    .or().like(SysUser::getNickname, keyword)
                    .or().like(SysUser::getPhone, keyword));
        }
        wrapper.like(nickname != null && !nickname.isBlank(), SysUser::getNickname, nickname)
                .eq(phone != null && !phone.isBlank(), SysUser::getPhone, phone)
                .ge(createdFrom != null, SysUser::getCreatedAt, createdFrom)
                .le(createdTo != null, SysUser::getCreatedAt, createdTo)
                .ge(lastLoginFrom != null, SysUser::getLastLoginTime, lastLoginFrom)
                .le(lastLoginTo != null, SysUser::getLastLoginTime, lastLoginTo);
        wrapper.orderByDesc(SysUser::getId);
        wrapper.select(SysUser.class, f -> !"passwordHash".equals(f.getProperty()));
        return Result.ok(userMapper.selectPage(
                new Page<>(page != null ? page : 1, size != null ? size : 20), wrapper));
    }

    @Operation(summary = "用户详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR','VIEWER')")
    public Result<SysUser> detail(@PathVariable Long id) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getId, id);
        wrapper.select(SysUser.class, f -> !"passwordHash".equals(f.getProperty()));
        return Result.ok(userMapper.selectOne(wrapper));
    }

    @Operation(summary = "切换用户禁用/启用状态", description = "兼容旧调用，只在正常和禁用之间切换；注销请使用状态修改接口。")
    @PutMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        SysUser user = requireUser(id);
        Integer before = user.getStatus();
        user.setStatus(user.getStatus() == 0 ? 1 : 0);
        user.setStatusReason(null);
        user.setDisabledAt(user.getStatus() == 0 ? null : LocalDateTime.now());
        user.setDisabledBy(user.getStatus() == 0 ? null : adminId());
        user.setTokenVersion((user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1);
        userMapper.updateById(user);
        adminLogService.log(adminId(), "STATUS", "USER", String.valueOf(id), before, user.getStatus());
        return Result.okMsg("状态已切换");
    }

    @Operation(summary = "修改用户基本资料", description = "允许修改昵称、邮箱、手机号和头像，不允许修改用户名。")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public Result<Void> update(@PathVariable Long id, @jakarta.validation.Valid @RequestBody UserUpdateRequest request) {
        SysUser user = requireUser(id);
        user.setNickname(request.getNickname()); user.setEmail(request.getEmail());
        user.setPhone(request.getPhone()); user.setAvatarUrl(request.getAvatarUrl());
        userMapper.updateById(user);
        adminLogService.log(adminId(), "UPDATE", "USER", String.valueOf(id), null, request);
        return Result.okMsg("用户资料已更新");
    }

    @Operation(summary = "修改用户账户状态", description = "状态：0-正常，1-禁用，2-注销。禁用或注销会立即使现有用户 Token 失效。")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> status(@PathVariable Long id, @jakarta.validation.Valid @RequestBody UserStatusRequest request) {
        if (request.getStatus() < 0 || request.getStatus() > 2) throw new BusinessException(ErrorCode.PARAM_ERROR, "用户状态无效");
        SysUser user = requireUser(id); Integer before = user.getStatus();
        user.setStatus(request.getStatus()); user.setStatusReason(request.getReason());
        user.setDisabledAt(request.getStatus() == 0 ? null : LocalDateTime.now());
        user.setDisabledBy(request.getStatus() == 0 ? null : adminId());
        user.setTokenVersion((user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1);
        userMapper.updateById(user);
        adminLogService.log(adminId(), "STATUS", "USER", String.valueOf(id), before, request);
        return Result.okMsg("用户状态已更新");
    }

    @Operation(summary = "管理员重置用户密码", description = "仅 ADMIN 可操作，重置后用户原有登录 Token 立即失效。")
    @PutMapping("/{id}/password")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> resetPassword(@PathVariable Long id,
                                      @jakarta.validation.Valid @RequestBody AdminResetPasswordRequest request) {
        SysUser user = requireUser(id);
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setTokenVersion((user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1);
        userMapper.updateById(user);
        adminLogService.log(adminId(), "RESET_PASSWORD", "USER", String.valueOf(id), null,
                java.util.Map.of("reason", request.getReason() == null ? "" : request.getReason()));
        return Result.okMsg("密码已重置");
    }

    @Operation(summary = "强制用户下线", description = "使用户当前所有访问 Token 和刷新 Token 失效。")
    @PostMapping("/{id}/force-logout")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> forceLogout(@PathVariable Long id) {
        SysUser user = requireUser(id);
        user.setTokenVersion((user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1);
        userMapper.updateById(user);
        adminLogService.log(adminId(), "FORCE_LOGOUT", "USER", String.valueOf(id), null, null);
        return Result.okMsg("用户已强制下线");
    }

    private SysUser requireUser(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        return user;
    }

    private Long adminId() { return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal(); }
}
