package com.project.module.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.project.common.response.Result;
import com.project.module.user.entity.SysUser;
import com.project.module.user.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理后台-用户管理", description = "用户列表、禁用/启用管理")
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserMapper userMapper;

    @Operation(summary = "用户列表")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR','VIEWER')")
    public Result<Page<SysUser>> list(
            @Parameter(description = "状态：0-正常 1-禁用") @RequestParam(required = false) Integer status,
            @Parameter(description = "关键词（用户名/邮箱模糊搜索）") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") Integer size) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(status != null, SysUser::getStatus, status);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getEmail, keyword));
        }
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

    @Operation(summary = "切换用户禁用/启用状态")
    @PutMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        SysUser user = userMapper.selectById(id);
        if (user != null) {
            user.setStatus(user.getStatus() == 1 ? 0 : 1);
            userMapper.updateById(user);
        }
        return Result.okMsg("状态已切换");
    }
}
