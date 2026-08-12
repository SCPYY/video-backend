package com.project.module.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.project.common.response.Result;
import com.project.module.admin.dto.ContentCreateRequest;
import com.project.module.admin.service.AdminContentService;
import com.project.module.content.entity.Content;
import com.project.module.content.dto.ContentDetailVO;
import com.project.module.admin.dto.ContentRejectRequest;
import com.project.module.content.entity.ContentStatusLog;
import java.util.List;
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
            @Parameter(description = "内容状态：0草稿 1待审核 2审核中 3待上架 4上架 5下架 7驳回") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer size) {
        return Result.ok(adminContentService.pageContents(type, status, page, size));
    }

    @Operation(summary = "审核待办列表", description = "仅管理员可查看待审核和审核中的内容，用于审核工作台。status 不传时默认查询 1-待审核、2-审核中。")
    @GetMapping("/review/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Page<Content>> reviewPending(
            @Parameter(description = "审核状态：1-待审核，2-审核中") @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        Integer reviewStatus = status == null ? 1 : status;
        if (reviewStatus != 1) {
            throw new com.project.common.exception.BusinessException(
                    com.project.common.exception.ErrorCode.PARAM_ERROR, "审核列表状态只能是1");
        }
        return Result.ok(adminContentService.pageContents(null, reviewStatus, page, size));
    }

    @Operation(summary = "创建内容")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR','VIEWER')")
    public Result<Content> create(@Valid @RequestBody ContentCreateRequest req) {
        return Result.ok(adminContentService.createContent(req, getAdminId()));
    }

    @Operation(summary = "管理端内容详情", description = "返回短剧或影游的完整基础信息、扩展属性、统计字段和剧集数量。")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR','VIEWER')")
    public Result<ContentDetailVO> detail(@PathVariable Long id) {
        return Result.ok(adminContentService.getAdminDetail(id));
    }

    @Operation(summary = "更新内容")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR','VIEWER')")
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
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        adminContentService.toggleStatus(id, getAdminId());
        return Result.okMsg("状态已切换");
    }

    @Operation(summary="提交审核（已停用）") @PostMapping("/{id}/submit-review") @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public Result<Void> submit(@PathVariable Long id) { adminContentService.submitReview(id,getAdminId()); return Result.okMsg("已提交审核"); }
    @Operation(summary="撤回审核") @PostMapping("/{id}/withdraw-review") @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public Result<Void> withdraw(@PathVariable Long id) { adminContentService.withdrawReview(id,getAdminId()); return Result.okMsg("已撤回审核"); }
    @Operation(summary="开始审核") @PostMapping("/{id}/start-review") @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> start(@PathVariable Long id) { adminContentService.startReview(id,getAdminId()); return Result.okMsg("已进入审核中"); }
    @Operation(summary="审核通过") @PostMapping("/{id}/approve") @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> approve(@PathVariable Long id) { adminContentService.approve(id,getAdminId()); return Result.okMsg("审核通过"); }
    @Operation(summary="审核驳回") @PostMapping("/{id}/reject") @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> reject(@PathVariable Long id,@Valid @RequestBody ContentRejectRequest req) { adminContentService.reject(id,getAdminId(),req.getReason()); return Result.okMsg("已驳回"); }
    @Operation(summary="发布上架") @PostMapping("/{id}/publish") @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> publish(@PathVariable Long id) { adminContentService.publish(id,getAdminId()); return Result.okMsg("已上架"); }
    @Operation(summary="下架内容") @PostMapping("/{id}/offline") @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> offline(@PathVariable Long id) { adminContentService.offline(id,getAdminId()); return Result.okMsg("已下架"); }
    @Operation(summary="状态变更历史") @GetMapping("/{id}/status-logs") @PreAuthorize("hasAnyRole('ADMIN','EDITOR','VIEWER')")
    public Result<List<ContentStatusLog>> logs(@PathVariable Long id) { return Result.ok(adminContentService.statusLogs(id)); }
}
