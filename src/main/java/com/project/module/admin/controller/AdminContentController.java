package com.project.module.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.project.common.response.Result;
import com.project.module.admin.dto.ContentCreateRequest;
import com.project.module.admin.service.AdminContentService;
import com.project.module.content.entity.Content;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理后台-内容管理", description = "内容的增删改查、上下架管理")
@RestController
@RequestMapping("/api/v1/admin/contents")
@RequiredArgsConstructor
public class AdminContentController {

    private final AdminContentService adminContentService;

    /**
     * 获取当前管理员ID
     */
    private Long getAdminId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long) {
            return (Long) auth.getPrincipal();
        }
        return null;
    }

    @Operation(summary = "内容列表")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR','VIEWER')")
    public Result<Page<Content>> list(
            @Parameter(description = "类型：1-短剧 2-影游") @RequestParam(required = false) Integer type,
            @Parameter(description = "状态：0-下架 1-上架") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer size) {
        return Result.ok(adminContentService.pageContents(type, status, page, size));
    }

    @Operation(summary = "创建内容")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public Result<Content> create(@Valid @RequestBody ContentCreateRequest req) {
        return Result.ok(adminContentService.createContent(req, getAdminId()));
    }

    @Operation(summary = "更新内容")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public Result<Content> update(@PathVariable Long id, @Valid @RequestBody ContentCreateRequest req) {
        return Result.ok(adminContentService.updateContent(id, req, getAdminId()));
    }

    @Operation(summary = "删除内容")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        adminContentService.deleteContent(id, getAdminId());
        return Result.okMsg("删除成功");
    }

    @Operation(summary = "切换上下架状态")
    @PutMapping("/{id}/toggle-status")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        adminContentService.toggleStatus(id, getAdminId());
        return Result.okMsg("状态已切换");
    }
}
