package com.project.module.content.controller;

import com.project.common.response.Result;
import com.project.module.content.dto.EpisodePlayVO;
import com.project.module.content.dto.EpisodeVO;
import com.project.module.content.service.EpisodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "剧集接口", description = "剧集列表、播放信息获取")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class EpisodeController {

    private final EpisodeService episodeService;

    @Operation(summary = "获取内容下的剧集列表")
    @GetMapping("/contents/{contentId}/episodes")
    public Result<List<EpisodeVO>> episodes(@PathVariable Long contentId) {
        return Result.ok(episodeService.getEpisodesByContentId(contentId));
    }

    @Operation(summary = "获取播放信息（含鉴权）")
    @GetMapping("/episodes/{id}/play")
    public Result<EpisodePlayVO> play(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = auth != null && auth.getPrincipal() instanceof Long
                ? (Long) auth.getPrincipal() : null;
        return Result.ok(episodeService.getPlayInfo(id, userId));
    }
}
