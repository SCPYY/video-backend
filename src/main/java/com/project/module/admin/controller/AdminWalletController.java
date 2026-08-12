package com.project.module.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.project.common.response.Result;
import com.project.module.wallet.dto.WalletAdjustRequest;
import com.project.module.wallet.dto.WalletTransactionVO;
import com.project.module.wallet.dto.WalletVO;
import com.project.module.wallet.service.WalletService;
import com.project.module.admin.dto.WalletCurrencyStatisticsVO;
import com.project.module.admin.mapper.WalletStatisticsMapper;
import com.project.module.admin.dto.WalletTransactionStatisticsVO;
import com.project.module.admin.mapper.WalletTransactionStatisticsMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Tag(name = "管理后台-钱包管理", description = "用户钱包余额、资金流水和人工调账")
@RestController
@RequestMapping("/api/v1/admin/wallets")
@RequiredArgsConstructor
public class AdminWalletController {
    private final WalletService walletService;
    private final WalletStatisticsMapper walletStatisticsMapper;
    private final WalletTransactionStatisticsMapper walletTransactionStatisticsMapper;

    @Operation(summary = "钱包流水统计", description = "统计充值、调账、支付、退款流水，并按 USD/EUR 分别汇总。")
    @GetMapping("/transactions/statistics")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR','VIEWER')")
    public Result<WalletTransactionStatisticsVO> transactionStatistics() {
        return Result.ok(walletTransactionStatisticsMapper.statistics());
    }

    @Operation(summary = "平台币钱包统计", description = "统计平台币钱包余额和状态数量。暂不进行法币换算，后续充值和入账模块再扩展汇率统计。")
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR','VIEWER')")
    public Result<WalletCurrencyStatisticsVO> statistics(
            @RequestParam(required = false) String targetCurrency,
            @RequestParam(required = false) BigDecimal eurToUsdRate) {
        WalletCurrencyStatisticsVO vo = walletStatisticsMapper.statistics();
        return Result.ok(vo);
    }

    @Operation(summary = "调整用户钱包余额", description = "增加或扣减用户 USD/EUR 钱包余额。仅 ADMIN 可操作，requestNo 用于幂等，操作会写入流水和管理员日志。")
    @PostMapping("/adjust")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<WalletVO> adjust(@Valid @RequestBody WalletAdjustRequest request) {
        Long adminId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return Result.ok(walletService.adjustBalance(adminId, request));
    }

    @Operation(summary = "钱包分页列表", description = "按币种和钱包状态查询用户钱包。状态：1-正常，其他值表示停用状态。")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR','VIEWER')")
    public Result<Page<WalletVO>> list(@RequestParam(required = false) String currency,
                                       @RequestParam(required = false) Integer status,
                                       @RequestParam(defaultValue = "1") Integer page,
                                       @RequestParam(defaultValue = "20") Integer size) {
        return Result.ok(walletService.pageAdminWallets(currency, status, page, size));
    }

    @Operation(summary = "钱包流水分页列表", description = "按用户、币种和流水类型查询资金变动记录。")
    @GetMapping("/transactions")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR','VIEWER')")
    public Result<Page<WalletTransactionVO>> transactions(@RequestParam(required = false) Long userId,
                                                          @RequestParam(required = false) String currency,
                                                          @RequestParam(required = false) String type,
                                                          @RequestParam(defaultValue = "1") Integer page,
                                                          @RequestParam(defaultValue = "20") Integer size) {
        return Result.ok(walletService.pageAdminTransactions(userId, currency, type, page, size));
    }
}
