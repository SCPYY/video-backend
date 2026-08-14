package com.project.module.content.controller;

import com.project.common.response.Result;
import com.project.module.content.dto.*;
import com.project.module.content.entity.InteractiveProgress;
import com.project.module.content.entity.InteractiveScene;
import com.project.module.content.service.InteractiveService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/interactive")
@RequiredArgsConstructor
public class InteractiveController {
    private final InteractiveService service;
    @GetMapping("/contents/{contentId}/tree") public Result<InteractiveTreeVO> tree(@PathVariable Long contentId){return Result.ok(service.tree(contentId));}
    @GetMapping("/scenes/{sceneId}/play") public Result<InteractiveScenePlayVO> play(@PathVariable Long sceneId,HttpServletRequest req){return Result.ok(service.playScene(sceneId,userId(),baseUrl(req)));}
    @PostMapping("/nodes/{nodeId}/choose") public Result<InteractiveChooseVO> choose(@PathVariable Long nodeId,@Valid @RequestBody InteractiveChooseRequest req){return Result.ok(service.choose(nodeId,req,userId()));}
    @GetMapping("/progress/{contentId}") public Result<InteractiveProgress> progress(@PathVariable Long contentId){return Result.ok(service.progress(contentId,userId()));}
    @PutMapping("/progress/{contentId}") public Result<Void> save(@PathVariable Long contentId,@RequestParam Long sceneId,@RequestParam(required=false) Long nodeId,@RequestParam(defaultValue="0") Integer seconds){service.saveProgress(contentId,userId(),sceneId,nodeId,seconds);return Result.okMsg("进度已保存");}
    @PostMapping("/progress/{contentId}/reset") public Result<Void> reset(@PathVariable Long contentId){service.reset(contentId,userId());return Result.okMsg("剧情已重置");}
    @GetMapping("/{contentId}/endings") public Result<List<InteractiveScene>> endings(@PathVariable Long contentId){return Result.ok(service.endings(contentId,userId()));}
    private Long userId(){var a=SecurityContextHolder.getContext().getAuthentication();return a!=null&&a.getPrincipal() instanceof Long?(Long)a.getPrincipal():null;}
    private String baseUrl(HttpServletRequest r){String b=r.getScheme()+"://"+r.getServerName();if(("http".equals(r.getScheme())&&r.getServerPort()!=80)||("https".equals(r.getScheme())&&r.getServerPort()!=443))b+=":"+r.getServerPort();return b;}
}
