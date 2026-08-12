package com.project.module.content.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.project.common.response.Result;
import com.project.module.content.entity.ContentCategory;
import com.project.module.content.mapper.ContentCategoryMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "内容分类", description = "短剧和影游分类查询")
@RestController
@RequestMapping({"/api/v1/content-categories", "/api/v1/contents/categories"})
@RequiredArgsConstructor
public class ContentCategoryController {
    private final ContentCategoryMapper mapper;

    @Operation(summary = "查询启用分类")
    @GetMapping
    public Result<List<ContentCategory>> list(@RequestParam(required = false) Integer type) {
        return Result.ok(mapper.selectList(new LambdaQueryWrapper<ContentCategory>()
                .eq(type != null, ContentCategory::getType, type)
                .eq(ContentCategory::getStatus, 1)
                .orderByAsc(ContentCategory::getSortOrder).orderByAsc(ContentCategory::getId)));
    }
}
