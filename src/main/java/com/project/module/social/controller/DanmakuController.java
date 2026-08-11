package com.project.module.social.controller;

import com.project.common.response.Result;
import com.project.module.social.dto.DanmakuRequest;
import com.project.module.social.dto.DanmakuVO;
import com.project.module.social.service.DanmakuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "弹幕接口", description = "弹幕发送、分段查询、点赞、删除")
@RestController
@RequestMapping("/api/v1/danmaku")
@RequiredArgsConstructor
public class DanmakuController {

    private final DanmakuService danmakuService;

    private Long getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long) {
            return (Long) auth.getPrincipal();
        }
        return null;
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities() != null) {
            return auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(r -> r.equals("ROLE_ADMIN") || r.equals("ROLE_EDITOR"));
        }
        return false;
    }

    @Operation(summary = "发送弹幕")
    @PostMapping
    public Result<DanmakuVO> send(@Valid @RequestBody DanmakuRequest request) {
        return Result.ok(danmakuService.sendDanmaku(request, getUserId()));
    }

    @Operation(summary = "获取弹幕列表（按时间段）",
            description = "按视频时间点分段查询弹幕，每段5秒。未登录亦可查看。")
    @GetMapping
    public Result<List<DanmakuVO>> list(
            @Parameter(description = "剧集ID") @RequestParam Long episodeId,
            @Parameter(description = "起始时间（秒）") @RequestParam Integer startTime,
            @Parameter(description = "结束时间（秒）") @RequestParam Integer endTime) {
        return Result.ok(danmakuService.getDanmakuList(episodeId, getUserId(), startTime, endTime));
    }

    @Operation(summary = "弹幕点赞/取消点赞")
    @PostMapping("/{id}/like")
    public Result<Boolean> like(@PathVariable Long id) {
        return Result.ok(danmakuService.toggleLike(id, getUserId()));
    }

    @Operation(summary = "删除弹幕（本人或管理员）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        danmakuService.deleteDanmaku(id, getUserId(), isAdmin());
        return Result.okMsg("已删除");
    }
}
