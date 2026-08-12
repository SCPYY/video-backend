package com.project.module.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.project.common.exception.BusinessException;
import com.project.common.exception.ErrorCode;
import com.project.common.response.Result;
import com.project.module.content.entity.*;
import com.project.module.content.mapper.*;
import com.project.module.admin.dto.InteractiveValidationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name="管理后台-影游互动剧情", description="影游场景、互动节点和选项跳转管理")
@RestController @RequestMapping("/api/v1/admin/interactive") @RequiredArgsConstructor
public class AdminInteractiveController {
    private final InteractiveSceneMapper sceneMapper;
    private final InteractiveNodeMapper nodeMapper;
    private final InteractiveOptionMapper optionMapper;

    @Operation(summary="校验影游剧情流程", description="发布前检查开始场景、结局场景、互动选项跳转目标和基础死循环风险。")
    @GetMapping("/validate")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR','VIEWER')")
    public Result<InteractiveValidationVO> validate(@RequestParam Long contentId) {
        List<InteractiveScene> scenes = sceneMapper.selectList(new LambdaQueryWrapper<InteractiveScene>().eq(InteractiveScene::getContentId, contentId));
        InteractiveValidationVO result = new InteractiveValidationVO();
        result.setSceneCount(scenes.size());
        if (scenes.isEmpty()) result.getErrors().add("未配置任何场景");
        long starts = scenes.stream().filter(s -> Integer.valueOf(1).equals(s.getIsStart())).count();
        long endings = scenes.stream().filter(s -> "ENDING".equalsIgnoreCase(s.getSceneType())).count();
        if (starts == 0) result.getErrors().add("未配置开始场景");
        if (starts > 1) result.getErrors().add("开始场景只能配置一个");
        if (endings == 0) result.getErrors().add("未配置结局场景");
        var sceneIds = scenes.stream().map(InteractiveScene::getId).collect(java.util.stream.Collectors.toSet());
        var nodes = scenes.isEmpty() ? List.<InteractiveNode>of() : nodeMapper.selectList(new LambdaQueryWrapper<InteractiveNode>().in(InteractiveNode::getSceneId, sceneIds));
        result.setNodeCount(nodes.size());
        var nodeIds = nodes.stream().map(InteractiveNode::getId).collect(java.util.stream.Collectors.toSet());
        var options = nodeIds.isEmpty() ? List.<InteractiveOption>of() : optionMapper.selectList(new LambdaQueryWrapper<InteractiveOption>().in(InteractiveOption::getNodeId, nodeIds));
        result.setOptionCount(options.size());
        for (InteractiveOption option : options) {
            if (option.getNextSceneId() == null && option.getNextNodeId() == null) result.getErrors().add("选项 " + option.getId() + " 未配置跳转目标");
            if (option.getNextSceneId() != null && !sceneIds.contains(option.getNextSceneId())) result.getErrors().add("选项 " + option.getId() + " 指向不存在的场景");
            if (option.getNextNodeId() != null && !nodeIds.contains(option.getNextNodeId())) result.getErrors().add("选项 " + option.getId() + " 指向不存在的节点");
        }
        if (options.isEmpty() && !scenes.isEmpty()) result.getWarnings().add("当前影游没有配置互动选项");
        result.setValid(result.getErrors().isEmpty());
        return Result.ok(result);
    }

    @Operation(summary="查询影游场景") @GetMapping("/scenes")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR','VIEWER')")
    public Result<List<InteractiveScene>> scenes(@RequestParam Long contentId) {
        return Result.ok(sceneMapper.selectList(new LambdaQueryWrapper<InteractiveScene>().eq(InteractiveScene::getContentId, contentId).orderByAsc(InteractiveScene::getSceneNo)));
    }
    @Operation(summary="创建影游场景") @PostMapping("/scenes") @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public Result<InteractiveScene> addScene(@Valid @RequestBody InteractiveScene item) { sceneMapper.insert(item); return Result.ok(item); }
    @Operation(summary="修改影游场景") @PutMapping("/scenes/{id}") @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public Result<InteractiveScene> updateScene(@PathVariable Long id, @RequestBody InteractiveScene item) { require(sceneMapper.selectById(id)); item.setId(id); sceneMapper.updateById(item); return Result.ok(sceneMapper.selectById(id)); }
    @Operation(summary="删除影游场景") @DeleteMapping("/scenes/{id}") @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteScene(@PathVariable Long id) { require(sceneMapper.selectById(id)); sceneMapper.deleteById(id); return Result.okMsg("场景已删除"); }

    @Operation(summary="查询场景互动节点") @GetMapping("/nodes") @PreAuthorize("hasAnyRole('ADMIN','EDITOR','VIEWER')")
    public Result<List<InteractiveNode>> nodes(@RequestParam Long sceneId) { return Result.ok(nodeMapper.selectList(new LambdaQueryWrapper<InteractiveNode>().eq(InteractiveNode::getSceneId, sceneId).orderByAsc(InteractiveNode::getNodeNo))); }
    @Operation(summary="创建互动节点") @PostMapping("/nodes") @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public Result<InteractiveNode> addNode(@RequestBody InteractiveNode item) { nodeMapper.insert(item); return Result.ok(item); }
    @Operation(summary="修改互动节点") @PutMapping("/nodes/{id}") @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public Result<InteractiveNode> updateNode(@PathVariable Long id, @RequestBody InteractiveNode item) { require(nodeMapper.selectById(id)); item.setId(id); nodeMapper.updateById(item); return Result.ok(nodeMapper.selectById(id)); }
    @Operation(summary="删除互动节点") @DeleteMapping("/nodes/{id}") @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteNode(@PathVariable Long id) { require(nodeMapper.selectById(id)); nodeMapper.deleteById(id); return Result.okMsg("节点已删除"); }

    @Operation(summary="查询互动选项") @GetMapping("/options") @PreAuthorize("hasAnyRole('ADMIN','EDITOR','VIEWER')")
    public Result<List<InteractiveOption>> options(@RequestParam Long nodeId) { return Result.ok(optionMapper.selectList(new LambdaQueryWrapper<InteractiveOption>().eq(InteractiveOption::getNodeId, nodeId).orderByAsc(InteractiveOption::getOptionNo))); }
    @Operation(summary="创建互动选项") @PostMapping("/options") @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public Result<InteractiveOption> addOption(@RequestBody InteractiveOption item) { optionMapper.insert(item); return Result.ok(item); }
    @Operation(summary="修改互动选项") @PutMapping("/options/{id}") @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public Result<InteractiveOption> updateOption(@PathVariable Long id, @RequestBody InteractiveOption item) { require(optionMapper.selectById(id)); item.setId(id); optionMapper.updateById(item); return Result.ok(optionMapper.selectById(id)); }
    @Operation(summary="删除互动选项") @DeleteMapping("/options/{id}") @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteOption(@PathVariable Long id) { require(optionMapper.selectById(id)); optionMapper.deleteById(id); return Result.okMsg("选项已删除"); }

    private void require(Object value) { if (value == null) throw new BusinessException(ErrorCode.PARAM_ERROR, "互动剧情数据不存在"); }
}
