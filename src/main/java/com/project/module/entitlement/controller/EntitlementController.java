package com.project.module.entitlement.controller;

import com.project.common.response.Result;
import com.project.module.entitlement.dto.EntitlementVO;
import com.project.module.entitlement.service.EntitlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "权益接口", description = "用户权益查询")
@RestController
@RequestMapping("/api/v1/entitlements")
@RequiredArgsConstructor
public class EntitlementController {

    private final EntitlementService entitlementService;

    private Long getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }

    @Operation(summary = "检查内容/剧集访问权限")
    @GetMapping("/check")
    public Result<Map<String, Boolean>> check(
            @Parameter(description = "内容ID") @RequestParam(required = false) Long contentId,
            @Parameter(description = "剧集ID") @RequestParam(required = false) Long episodeId) {
        boolean hasAccess = entitlementService.checkAccess(getUserId(), contentId, episodeId);
        Map<String, Boolean> result = new HashMap<>();
        result.put("hasAccess", hasAccess);
        return Result.ok(result);
    }

    @Operation(summary = "用户权益列表")
    @GetMapping("/list")
    public Result<List<EntitlementVO>> list() {
        return Result.ok(entitlementService.listUserEntitlements(getUserId()));
    }
}
