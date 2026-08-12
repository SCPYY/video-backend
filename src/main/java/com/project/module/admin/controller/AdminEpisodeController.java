package com.project.module.admin.controller;

import com.project.common.response.Result;
import com.project.module.admin.dto.BatchAddEpisodesRequest;
import com.project.module.admin.dto.SortEpisodesRequest;
import com.project.module.admin.service.AdminEpisodeService;
import com.project.module.content.entity.Episode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "管理后台-剧集管理", description = "剧集的增删改查、批量添加、排序")
@RestController
@RequestMapping("/api/v1/admin/episodes")
@RequiredArgsConstructor
public class AdminEpisodeController {

    private final AdminEpisodeService adminEpisodeService;

    @Operation(summary = "查询内容剧集列表")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR','VIEWER')")
    public Result<List<Episode>> list(@RequestParam Long contentId) {
        return Result.ok(adminEpisodeService.listEpisodes(contentId));
    }

    @Operation(summary = "查询剧集详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR','VIEWER')")
    public Result<Episode> detail(@PathVariable Long id) {
        return Result.ok(adminEpisodeService.getEpisode(id));
    }

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

    @Operation(summary = "添加单集")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public Result<Episode> add(@Valid @RequestBody Episode episode) {
        return Result.ok(adminEpisodeService.addEpisode(episode, getAdminId()));
    }

    @Operation(summary = "更新单集")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public Result<Episode> update(@PathVariable Long id, @Valid @RequestBody Episode episode) {
        return Result.ok(adminEpisodeService.updateEpisode(id, episode, getAdminId()));
    }

    @Operation(summary = "删除单集")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public Result<Void> delete(@PathVariable Long id) {
        adminEpisodeService.deleteEpisode(id, getAdminId());
        return Result.okMsg("删除成功");
    }

    @Operation(summary = "批量添加剧集")
    @PostMapping("/batch")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public Result<Integer> batchAdd(@Valid @RequestBody BatchAddEpisodesRequest request) {
        return Result.ok(adminEpisodeService.batchAddEpisodes(request, getAdminId()));
    }

    @Operation(summary = "调整剧集排序")
    @PutMapping("/sort")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public Result<Void> sort(@Valid @RequestBody SortEpisodesRequest request) {
        adminEpisodeService.sortEpisodes(request, getAdminId());
        return Result.okMsg("排序更新成功");
    }
}
