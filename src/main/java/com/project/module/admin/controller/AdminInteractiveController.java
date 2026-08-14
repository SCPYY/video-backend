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
import java.util.Map;

@Tag(name="管理后台-影游互动剧情", description="影游场景、互动节点和选项跳转管理")
@RestController @RequestMapping("/api/v1/admin/interactive") @RequiredArgsConstructor
public class AdminInteractiveController {
    private final InteractiveSceneMapper sceneMapper;
    private final InteractiveNodeMapper nodeMapper;
    private final InteractiveOptionMapper optionMapper;
    private final ContentMapper contentMapper;

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
        for (InteractiveScene scene : scenes) {
            if (scene.getVideoUrl() == null || scene.getVideoUrl().isBlank()) result.getErrors().add("场景 " + scene.getId() + " 未配置视频");
        }
        Map<Long, Long> optionCounts = options.stream().collect(java.util.stream.Collectors.groupingBy(InteractiveOption::getNodeId, java.util.stream.Collectors.counting()));
        Map<Long, InteractiveScene> sceneMap = scenes.stream().collect(java.util.stream.Collectors.toMap(InteractiveScene::getId, s -> s));
        for (InteractiveNode node : nodes) {
            if (optionCounts.getOrDefault(node.getId(), 0L) < 2) result.getErrors().add("节点 " + node.getId() + " 至少需要两个有效选项");
            InteractiveScene scene = sceneMap.get(node.getSceneId());
            if (node.getShowAt() != null && scene != null && scene.getDuration() != null && node.getShowAt() > scene.getDuration()) result.getErrors().add("节点 " + node.getId() + " 触发时间超过视频时长");
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
    public Result<InteractiveScene> addScene(@Valid @RequestBody InteractiveScene item) { validateScene(item,null); sceneMapper.insert(item); return Result.ok(item); }
    @Operation(summary="修改影游场景") @PutMapping("/scenes/{id}") @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public Result<InteractiveScene> updateScene(@PathVariable Long id, @RequestBody InteractiveScene item) { require(sceneMapper.selectById(id)); validateScene(item,id); item.setId(id); sceneMapper.updateById(item); return Result.ok(sceneMapper.selectById(id)); }
    @Operation(summary="删除影游场景") @DeleteMapping("/scenes/{id}") @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteScene(@PathVariable Long id) { require(sceneMapper.selectById(id)); if(optionMapper.selectCount(new LambdaQueryWrapper<InteractiveOption>().eq(InteractiveOption::getNextSceneId,id))>0)throw new BusinessException(ErrorCode.PARAM_ERROR,"场景正在被剧情选项引用，不能删除");if(nodeMapper.selectCount(new LambdaQueryWrapper<InteractiveNode>().eq(InteractiveNode::getSceneId,id))>0)throw new BusinessException(ErrorCode.PARAM_ERROR,"场景下存在互动节点，请先删除节点");sceneMapper.deleteById(id); return Result.okMsg("场景已删除"); }

    @Operation(summary="查询场景互动节点") @GetMapping("/nodes") @PreAuthorize("hasAnyRole('ADMIN','EDITOR','VIEWER')")
    public Result<List<InteractiveNode>> nodes(@RequestParam Long sceneId) { return Result.ok(nodeMapper.selectList(new LambdaQueryWrapper<InteractiveNode>().eq(InteractiveNode::getSceneId, sceneId).orderByAsc(InteractiveNode::getNodeNo))); }
    @Operation(summary="创建互动节点") @PostMapping("/nodes") @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public Result<InteractiveNode> addNode(@RequestBody InteractiveNode item) { validateNode(item); nodeMapper.insert(item); return Result.ok(item); }
    @Operation(summary="修改互动节点") @PutMapping("/nodes/{id}") @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public Result<InteractiveNode> updateNode(@PathVariable Long id, @RequestBody InteractiveNode item) { require(nodeMapper.selectById(id)); validateNode(item); item.setId(id); nodeMapper.updateById(item); return Result.ok(nodeMapper.selectById(id)); }
    @Operation(summary="删除互动节点") @DeleteMapping("/nodes/{id}") @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteNode(@PathVariable Long id) { require(nodeMapper.selectById(id)); if(optionMapper.selectCount(new LambdaQueryWrapper<InteractiveOption>().eq(InteractiveOption::getNodeId,id).or().eq(InteractiveOption::getNextNodeId,id))>0)throw new BusinessException(ErrorCode.PARAM_ERROR,"节点存在选项或正在被引用，不能删除");nodeMapper.deleteById(id); return Result.okMsg("节点已删除"); }

    @Operation(summary="查询互动选项") @GetMapping("/options") @PreAuthorize("hasAnyRole('ADMIN','EDITOR','VIEWER')")
    public Result<List<InteractiveOption>> options(@RequestParam Long nodeId) { return Result.ok(optionMapper.selectList(new LambdaQueryWrapper<InteractiveOption>().eq(InteractiveOption::getNodeId, nodeId).orderByAsc(InteractiveOption::getOptionNo))); }
    @Operation(summary="创建互动选项") @PostMapping("/options") @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public Result<InteractiveOption> addOption(@RequestBody InteractiveOption item) { validateOption(item); optionMapper.insert(item); return Result.ok(item); }
    @Operation(summary="修改互动选项") @PutMapping("/options/{id}") @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public Result<InteractiveOption> updateOption(@PathVariable Long id, @RequestBody InteractiveOption item) { require(optionMapper.selectById(id)); validateOption(item); item.setId(id); optionMapper.updateById(item); return Result.ok(optionMapper.selectById(id)); }
    @Operation(summary="删除互动选项") @DeleteMapping("/options/{id}") @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteOption(@PathVariable Long id) { require(optionMapper.selectById(id)); optionMapper.deleteById(id); return Result.okMsg("选项已删除"); }

    private void require(Object value) { if (value == null) throw new BusinessException(ErrorCode.PARAM_ERROR, "互动剧情数据不存在"); }
    private void validateScene(InteractiveScene item, Long excludeId) {
        Content content = contentMapper.selectById(item.getContentId());
        if (content == null || !Integer.valueOf(2).equals(content.getType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "场景必须关联有效影游内容");
        }
        if (item.getSceneNo() == null || item.getTitle() == null || item.getTitle().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "场景编号和标题不能为空");
        }
        if (Integer.valueOf(1).equals(item.getIsStart())) {
            LambdaQueryWrapper<InteractiveScene> wrapper = new LambdaQueryWrapper<InteractiveScene>()
                    .eq(InteractiveScene::getContentId, item.getContentId())
                    .eq(InteractiveScene::getIsStart, 1)
                    .ne(excludeId != null, InteractiveScene::getId, excludeId);
            if (sceneMapper.selectCount(wrapper) > 0) throw new BusinessException(ErrorCode.PARAM_ERROR, "一个影游只能有一个开始场景");
        }
        if (item.getStatus() == null) item.setStatus(1);
        if (item.getIsStart() == null) item.setIsStart(0);
        if (item.getSceneType() == null || item.getSceneType().isBlank()) item.setSceneType("NORMAL");
    }

    private void validateOption(InteractiveOption item) {
        InteractiveNode node = nodeMapper.selectById(item.getNodeId());
        if (node == null) throw new BusinessException(ErrorCode.PARAM_ERROR, "选项必须关联有效互动节点");
        if ((item.getNextSceneId() == null) == (item.getNextNodeId() == null)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "选项必须且只能配置一个跳转目标");
        }
        InteractiveScene sourceScene = sceneMapper.selectById(node.getSceneId());
        if (item.getNextSceneId() != null) {
            InteractiveScene target = sceneMapper.selectById(item.getNextSceneId());
            if (target == null || !sourceScene.getContentId().equals(target.getContentId())) throw new BusinessException(ErrorCode.PARAM_ERROR, "目标场景无效");
        }
        if (item.getNextNodeId() != null) {
            InteractiveNode targetNode = nodeMapper.selectById(item.getNextNodeId());
            InteractiveScene targetScene = targetNode == null ? null : sceneMapper.selectById(targetNode.getSceneId());
            if (targetScene == null || !sourceScene.getContentId().equals(targetScene.getContentId())) throw new BusinessException(ErrorCode.PARAM_ERROR, "目标节点无效");
        }
        if (item.getStatus() == null) item.setStatus(1);
    }

    private void validateNode(InteractiveNode item) {
        InteractiveScene scene = sceneMapper.selectById(item.getSceneId());
        if (scene == null) throw new BusinessException(ErrorCode.PARAM_ERROR, "节点必须关联有效场景");
        if (item.getNodeNo() == null || item.getPrompt() == null || item.getPrompt().isBlank()) throw new BusinessException(ErrorCode.PARAM_ERROR, "节点编号和提示语不能为空");
        if (item.getShowAt() != null && item.getShowAt() < 0) throw new BusinessException(ErrorCode.PARAM_ERROR, "触发时间不能小于0");
        if (item.getShowAt() != null && scene.getDuration() != null && item.getShowAt() > scene.getDuration()) throw new BusinessException(ErrorCode.PARAM_ERROR, "节点触发时间不能超过场景视频时长");
    }
}
