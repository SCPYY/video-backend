package com.project.module.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.project.common.response.Result;
import com.project.module.admin.dto.GrantEntitlementRequest;
import com.project.module.entitlement.dto.EntitlementVO;
import com.project.module.entitlement.service.EntitlementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "管理后台-权益管理", description = "用户权益查询、人工发放和撤销")
@RestController
@RequestMapping("/api/v1/admin/entitlements")
@RequiredArgsConstructor
public class AdminEntitlementController {
    private final EntitlementService entitlementService;

    @GetMapping
    @Operation(summary = "权益分页列表", description = "按用户 ID 和权益类型查询用户内容权益。类型：1-内容解锁，2-会员。")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR','VIEWER')")
    public Result<Page<EntitlementVO>> list(@Parameter(description = "用户 ID") @RequestParam(required = false) Long userId,
                                            @Parameter(description = "权益类型：1-内容解锁，2-会员") @RequestParam(required = false) Integer type,
                                            @RequestParam(defaultValue = "1") Integer page,
                                            @RequestParam(defaultValue = "20") Integer size) {
        return Result.ok(entitlementService.pageAdminEntitlements(userId, type, page, size));
    }

    @PostMapping("/grant")
    @Operation(summary = "人工发放权益", description = "根据商品配置向用户发放对应权益，仅 ADMIN 可操作，并记录管理员操作日志。")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<EntitlementVO> grant(@Valid @RequestBody GrantEntitlementRequest request) {
        Long adminId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return Result.ok(entitlementService.grantByProduct(request.getUserId(), request.getProductId(), adminId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "撤销权益", description = "撤销指定权益，仅 ADMIN 可操作。撤销前应确认对应订单和业务原因。")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> revoke(@PathVariable Long id) {
        Long adminId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        entitlementService.revoke(id, adminId);
        return Result.okMsg("权益已撤销");
    }
}
