package com.project.module.admin.controller;

import com.project.common.response.Result;
import com.project.module.admin.dto.DashboardSummaryVO;
import com.project.module.admin.mapper.DashboardMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "管理后台-数据看板", description = "管理端核心业务汇总指标")
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {
    private final DashboardMapper dashboardMapper;

    @GetMapping("/summary")
    @Operation(summary = "看板汇总指标", description = "返回用户、内容、订单、收入、待处理举报和近 7 天搜索次数等首页卡片数据。")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR','VIEWER')")
    public Result<DashboardSummaryVO> summary() {
        return Result.ok(dashboardMapper.summary());
    }
}
