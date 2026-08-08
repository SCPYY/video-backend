package com.project.module.content.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.project.common.response.Result;
import com.project.module.content.dto.ContentDetailVO;
import com.project.module.content.dto.ContentListItemVO;
import com.project.module.content.dto.ContentListQuery;
import com.project.module.content.service.ContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "内容接口", description = "内容列表、详情、剧集浏览")
@RestController
@RequestMapping("/api/v1/contents")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;

    @Operation(summary = "内容列表（分页+筛选）")
    @GetMapping
    public Result<Page<ContentListItemVO>> list(ContentListQuery query) {
        return Result.ok(contentService.pageContent(query));
    }

    @Operation(summary = "热门内容")
    @GetMapping("/hot")
    public Result<List<ContentListItemVO>> hot(
            @Parameter(description = "返回条数，默认8") @RequestParam(defaultValue = "8") Integer limit) {
        return Result.ok(contentService.hotContent(limit));
    }

    @Operation(summary = "内容详情（含扩展属性）")
    @GetMapping("/{id}")
    public Result<ContentDetailVO> detail(@PathVariable Long id) {
        return Result.ok(contentService.getDetail(id));
    }
}
