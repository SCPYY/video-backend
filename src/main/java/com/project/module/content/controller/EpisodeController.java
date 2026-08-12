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
import jakarta.servlet.http.HttpServletRequest;

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
    public Result<EpisodePlayVO> play(@PathVariable Long id, HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = auth != null && auth.getPrincipal() instanceof Long
                ? (Long) auth.getPrincipal() : null;
        String visitorKey = request.getRemoteAddr() + "|" + request.getHeader("User-Agent");
        EpisodePlayVO vo = episodeService.getPlayInfo(id, userId, visitorKey);
        if (vo.getVideoUrl() != null && vo.getVideoUrl().startsWith("/")) {
            String base = request.getScheme() + "://" + request.getServerName();
            if ((request.getScheme().equals("http") && request.getServerPort() != 80)
                    || (request.getScheme().equals("https") && request.getServerPort() != 443)) {
                base += ":" + request.getServerPort();
            }
            vo.setVideoUrl(base + vo.getVideoUrl());
        }
        return Result.ok(vo);
    }
}
