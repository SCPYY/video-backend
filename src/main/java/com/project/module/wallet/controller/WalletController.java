package com.project.module.wallet.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.project.common.response.Result;
import com.project.module.wallet.dto.WalletTransactionVO;
import com.project.module.wallet.dto.WalletVO;
import com.project.module.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "钱包接口", description = "钱包余额和资金流水查询")
@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @Operation(summary = "查询当前用户钱包")
    @GetMapping
    public Result<List<WalletVO>> listWallets() {
        return Result.ok(walletService.listWallets(getUserId()));
    }

    @Operation(summary = "查询钱包流水")
    @GetMapping("/transactions")
    public Result<Page<WalletTransactionVO>> transactions(
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return Result.ok(walletService.pageTransactions(getUserId(), currency, type, page, size));
    }

    private Long getUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
