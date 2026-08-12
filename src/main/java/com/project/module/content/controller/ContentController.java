package com.project.module.content.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.project.common.response.Result;
import com.project.module.content.dto.ContentDetailVO;
import com.project.module.content.dto.ContentListItemVO;
import com.project.module.content.dto.ContentListQuery;
import com.project.module.content.dto.HotTagVO;
import com.project.module.content.dto.HotContentVO;
import com.project.module.content.service.ContentService;
import com.project.module.content.service.SearchLogService;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

@Tag(name = "内容接口", description = "内容列表、详情、剧集浏览")
@RestController
@RequestMapping("/api/v1/contents")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;
    private final SearchLogService searchLogService;

    @Operation(summary = "内容列表（分页+筛选）")
    @GetMapping
    public Result<Page<ContentListItemVO>> list(ContentListQuery query, HttpServletRequest request) {
        Page<ContentListItemVO> result = contentService.pageContent(query);
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long userId = authentication != null && authentication.getPrincipal() instanceof Long
                    ? (Long) authentication.getPrincipal() : null;
            searchLogService.record(userId, query.getKeyword(), result.getTotal(), request);
        }
        return Result.ok(result);
    }

    @Operation(summary = "首页轮播：最热短剧Top6")
    @GetMapping("/carousel")
    public Result<List<ContentListItemVO>> carousel() {
        return Result.ok(contentService.carouselContent());
    }

    @Operation(summary = "按浏览量排序的热门短剧/影游")
    @GetMapping("/hot")
    public Result<List<HotContentVO>> hot(
            @Parameter(description = "热门内容返回条数，默认8，最大20")
            @RequestParam(defaultValue = "8") Integer limit) {
        return Result.ok(searchLogService.hotContents(limit));
    }

    @Operation(summary = "热门标签")
    @GetMapping("/tags/hot")
    public Result<List<HotTagVO>> hotTags(
            @Parameter(description = "返回条数，默认10，最大50")
            @RequestParam(defaultValue = "10") Integer limit,
            @Parameter(description = "统计最近天数，默认7，最大90")
            @RequestParam(defaultValue = "7") Integer days) {
        return Result.ok(searchLogService.hotTags(limit, days));
    }

    @Operation(summary = "内容详情（含扩展属性）")
    @GetMapping("/{id}")
    public Result<ContentDetailVO> detail(@PathVariable Long id, HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = auth != null && auth.getPrincipal() instanceof Long ? (Long) auth.getPrincipal() : null;
        String visitorKey = request.getRemoteAddr() + "|" + request.getHeader("User-Agent");
        return Result.ok(contentService.getDetail(id, userId, visitorKey));
    }
}
