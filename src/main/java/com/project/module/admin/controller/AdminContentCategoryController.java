package com.project.module.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.project.common.exception.BusinessException;
import com.project.common.exception.ErrorCode;
import com.project.common.response.Result;
import com.project.module.content.entity.ContentCategory;
import com.project.module.content.mapper.ContentCategoryMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "管理后台-内容分类", description = "短剧和影游分类管理")
@RestController
@RequestMapping("/api/v1/admin/content-categories")
@RequiredArgsConstructor
public class AdminContentCategoryController {
    private final ContentCategoryMapper mapper;

    @Operation(summary = "分类列表")
    @GetMapping({"", "/list"})
    @PreAuthorize("isAuthenticated()")
    public Result<List<ContentCategory>> list(@RequestParam(required = false) Integer type,
                                               @RequestParam(required = false) Integer status) {
        return Result.ok(mapper.selectList(new LambdaQueryWrapper<ContentCategory>()
                .eq(type != null, ContentCategory::getType, type)
                .eq(status != null, ContentCategory::getStatus, status)
                .orderByAsc(ContentCategory::getType).orderByAsc(ContentCategory::getSortOrder)));
    }

    @Operation(summary = "新增分类")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public Result<ContentCategory> create(@Valid @RequestBody ContentCategory category) {
        validate(category);
        if (mapper.selectCount(new LambdaQueryWrapper<ContentCategory>().eq(ContentCategory::getType, category.getType()).eq(ContentCategory::getName, category.getName())) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "同类型下分类名称已存在");
        }
        if (category.getStatus() == null) category.setStatus(1);
        if (category.getSortOrder() == null) category.setSortOrder(0);
        mapper.insert(category);
        return Result.ok(category);
    }

    @Operation(summary = "修改分类")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public Result<ContentCategory> update(@PathVariable Long id, @RequestBody ContentCategory category) {
        ContentCategory existing = mapper.selectById(id);
        if (existing == null) throw new BusinessException(ErrorCode.PARAM_ERROR, "分类不存在");
        validate(category);
        category.setId(id);
        mapper.updateById(category);
        return Result.ok(mapper.selectById(id));
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        if (mapper.selectById(id) == null) throw new BusinessException(ErrorCode.PARAM_ERROR, "分类不存在");
        mapper.deleteById(id);
        return Result.okMsg("分类已删除");
    }

    private void validate(ContentCategory category) {
        if (category.getType() == null || (category.getType() != 1 && category.getType() != 2)
                || category.getName() == null || category.getName().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "分类类型或名称不合法");
        }
    }
}
