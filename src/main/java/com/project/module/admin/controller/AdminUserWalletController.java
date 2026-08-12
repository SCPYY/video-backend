package com.project.module.admin.controller;

import com.project.common.response.Result;
import com.project.module.admin.dto.WalletStatusRequest;
import com.project.module.wallet.dto.WalletTransactionVO;
import com.project.module.wallet.dto.WalletVO;
import com.project.module.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;

@Tag(name = "管理后台-用户钱包", description = "按用户查看钱包账户、流水并管理钱包状态")
@RestController
@RequestMapping("/api/v1/admin/users/{userId}/wallets")
@RequiredArgsConstructor
public class AdminUserWalletController {
    private final WalletService walletService;

    @Operation(summary = "查询用户钱包账户")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR','VIEWER')")
    public Result<List<WalletVO>> list(@PathVariable Long userId) {
        return Result.ok(walletService.listAdminUserWallets(userId));
    }

    @Operation(summary = "查询用户钱包流水")
    @GetMapping("/{currency}/transactions")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR','VIEWER')")
    public Result<Page<WalletTransactionVO>> transactions(@PathVariable Long userId, @PathVariable String currency,
                                                           @RequestParam(required = false) String type,
                                                           @RequestParam(defaultValue = "1") Integer page,
                                                           @RequestParam(defaultValue = "20") Integer size) {
        return Result.ok(walletService.pageAdminTransactions(userId, currency, type, page, size));
    }

    @Operation(summary = "变更用户钱包状态", description = "1正常、2冻结、3关闭；关闭钱包前余额必须为零。冻结钱包不能支付或扣款，但允许退款入账。")
    @PutMapping("/{currency}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<WalletVO> status(@PathVariable Long userId, @PathVariable String currency,
                                   @Valid @RequestBody WalletStatusRequest request) {
        Long adminId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return Result.ok(walletService.updateWalletStatus(adminId, userId, currency, request.getStatus(), request.getReason()));
    }
}
