package com.project.module.admin.controller;

import com.project.module.admin.export.AdminExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.Map;

@Tag(name = "管理端-通用表格导出")
@RestController
@RequestMapping("/api/v1/admin/exports")
@RequiredArgsConstructor
public class AdminExportController {
    private final AdminExportService exportService;

    @GetMapping("/{resource}")
    @Operation(summary = "导出管理端表格", description = "resource支持users、contents、orders、products、entitlements、wallets、wallet-transactions、admin-logs；format支持csv、json、xlsx、pdf。可复用keyword、status、type、userId、startTime、endTime等筛选参数，当前默认最多导出10000条。")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR','VIEWER')")
    public void export(@PathVariable String resource, @RequestParam(defaultValue = "csv") String format,
                       @RequestParam Map<String, String> params, HttpServletResponse response) throws IOException {
        try {
            exportService.export(resource, format, params, response, null);
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }
}
