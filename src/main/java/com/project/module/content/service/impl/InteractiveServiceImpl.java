package com.project.module.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.project.common.exception.BusinessException;
import com.project.common.exception.ErrorCode;
import com.project.module.content.dto.*;
import com.project.module.content.entity.*;
import com.project.module.content.mapper.*;
import com.project.module.content.service.InteractiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InteractiveServiceImpl implements InteractiveService {
    private final ContentMapper contentMapper;
    private final InteractiveSceneMapper sceneMapper;
    private final InteractiveNodeMapper nodeMapper;
    private final InteractiveOptionMapper optionMapper;
    private final InteractiveProgressMapper progressMapper;
    private final InteractiveChoiceMapper choiceMapper;

    @Override public InteractiveTreeVO tree(Long contentId) {
        requireOnlineInteractive(contentId);
        List<InteractiveScene> scenes = sceneMapper.selectList(new LambdaQueryWrapper<InteractiveScene>()
                .eq(InteractiveScene::getContentId, contentId).eq(InteractiveScene::getStatus, 1).orderByAsc(InteractiveScene::getSceneNo));
        InteractiveTreeVO result = new InteractiveTreeVO(); result.setContentId(contentId);
        scenes.stream().filter(s -> Integer.valueOf(1).equals(s.getIsStart())).findFirst().ifPresent(s -> result.setStartSceneId(s.getId()));
        Set<Long> sceneIds = scenes.stream().map(InteractiveScene::getId).collect(Collectors.toSet());
        List<InteractiveNode> nodes = sceneIds.isEmpty() ? List.of() : nodeMapper.selectList(new LambdaQueryWrapper<InteractiveNode>().in(InteractiveNode::getSceneId, sceneIds).orderByAsc(InteractiveNode::getNodeNo));
        Set<Long> nodeIds = nodes.stream().map(InteractiveNode::getId).collect(Collectors.toSet());
        List<InteractiveOption> options = nodeIds.isEmpty() ? List.of() : optionMapper.selectList(new LambdaQueryWrapper<InteractiveOption>().in(InteractiveOption::getNodeId, nodeIds).eq(InteractiveOption::getStatus, 1).orderByAsc(InteractiveOption::getOptionNo));
        Map<Long,List<InteractiveOption>> optionMap = options.stream().collect(Collectors.groupingBy(InteractiveOption::getNodeId));
        Map<Long,List<InteractiveNode>> nodeMap = nodes.stream().collect(Collectors.groupingBy(InteractiveNode::getSceneId));
        for (InteractiveScene scene : scenes) { InteractiveScene safeScene=new InteractiveScene();org.springframework.beans.BeanUtils.copyProperties(scene,safeScene);safeScene.setVideoUrl(null);InteractiveTreeVO.SceneItem si=new InteractiveTreeVO.SceneItem();si.setScene(safeScene);for(InteractiveNode node:nodeMap.getOrDefault(scene.getId(),List.of())){InteractiveTreeVO.NodeItem ni=new InteractiveTreeVO.NodeItem();ni.setNode(node);ni.setOptions(optionMap.getOrDefault(node.getId(),List.of()));si.getNodes().add(ni);}result.getScenes().add(si); }
        return result;
    }

    @Override @Transactional public InteractiveScenePlayVO playScene(Long sceneId, Long userId, String baseUrl) {
        InteractiveScene scene=requireScene(sceneId); requireOnlineInteractive(scene.getContentId());
        InteractiveScenePlayVO vo=new InteractiveScenePlayVO();vo.setId(scene.getId());vo.setContentId(scene.getContentId());vo.setTitle(scene.getTitle());vo.setDescription(scene.getDescription());vo.setDuration(scene.getDuration());vo.setSceneType(scene.getSceneType());vo.setEnding("ENDING".equalsIgnoreCase(scene.getSceneType()));
        String url=scene.getVideoUrl();vo.setVideoUrl(url!=null&&url.startsWith("/")?baseUrl+url:url);
        vo.setFirstNode(nodeMapper.selectOne(new LambdaQueryWrapper<InteractiveNode>().eq(InteractiveNode::getSceneId,sceneId).orderByAsc(InteractiveNode::getNodeNo).last("LIMIT 1")));
        InteractiveScene update=new InteractiveScene();update.setId(sceneId);update.setPlayCount((scene.getPlayCount()==null?0:scene.getPlayCount())+1);sceneMapper.updateById(update);
        if(userId!=null) saveProgress(scene.getContentId(),userId,sceneId,vo.getFirstNode()==null?null:vo.getFirstNode().getId(),0);
        return vo;
    }

    @Override @Transactional public InteractiveChooseVO choose(Long nodeId, InteractiveChooseRequest request, Long userId) {
        if(userId==null) throw new BusinessException(ErrorCode.TOKEN_INVALID,"请登录后选择剧情");
        InteractiveNode node=nodeMapper.selectById(nodeId);if(node==null)throw new BusinessException(ErrorCode.PARAM_ERROR,"互动节点不存在");
        InteractiveScene scene=requireScene(node.getSceneId());requireOnlineInteractive(scene.getContentId());
        InteractiveOption option=optionMapper.selectById(request.getOptionId());if(option==null||!nodeId.equals(option.getNodeId())||!Integer.valueOf(1).equals(option.getStatus()))throw new BusinessException(ErrorCode.PARAM_ERROR,"互动选项无效");
        InteractiveScene nextScene=option.getNextSceneId()==null?null:requireScene(option.getNextSceneId());
        if(nextScene!=null&&!scene.getContentId().equals(nextScene.getContentId()))throw new BusinessException(ErrorCode.PARAM_ERROR,"不能跳转到其他影游场景");
        InteractiveChoice choice=new InteractiveChoice();choice.setUserId(userId);choice.setContentId(scene.getContentId());choice.setSceneId(scene.getId());choice.setNodeId(nodeId);choice.setOptionId(option.getId());choice.setNextSceneId(option.getNextSceneId());choice.setNextNodeId(option.getNextNodeId());choice.setVideoPosition(request.getPosition());choiceMapper.insert(choice);
        boolean ending=nextScene!=null&&"ENDING".equalsIgnoreCase(nextScene.getSceneType());
        InteractiveProgress progress=getOrCreate(scene.getContentId(),userId);progress.setCurrentSceneId(option.getNextSceneId()!=null?option.getNextSceneId():scene.getId());progress.setCurrentNodeId(option.getNextNodeId());progress.setLastOptionId(option.getId());progress.setProgressSeconds(0);progress.setIsFinished(ending?1:0);progress.setEndingSceneId(ending?nextScene.getId():null);progress.setLastPlayedAt(LocalDateTime.now());progressMapper.updateById(progress);
        InteractiveChooseVO vo=new InteractiveChooseVO();vo.setNextSceneId(option.getNextSceneId());vo.setNextNodeId(option.getNextNodeId());vo.setEnding(ending);vo.setEndingSceneId(ending?nextScene.getId():null);return vo;
    }

    @Override public InteractiveProgress progress(Long contentId,Long userId){if(userId==null)throw new BusinessException(ErrorCode.TOKEN_INVALID);requireOnlineInteractive(contentId);return getOrCreate(contentId,userId);}
    @Override @Transactional public void saveProgress(Long contentId,Long userId,Long sceneId,Long nodeId,Integer seconds){if(userId==null)return;InteractiveScene scene=requireScene(sceneId);if(!contentId.equals(scene.getContentId()))throw new BusinessException(ErrorCode.PARAM_ERROR,"场景不属于当前影游");InteractiveProgress p=getOrCreate(contentId,userId);p.setCurrentSceneId(sceneId);p.setCurrentNodeId(nodeId);p.setProgressSeconds(Math.max(0,seconds==null?0:seconds));p.setLastPlayedAt(LocalDateTime.now());progressMapper.updateById(p);}
    @Override @Transactional public void reset(Long contentId,Long userId){if(userId==null)throw new BusinessException(ErrorCode.TOKEN_INVALID);InteractiveScene start=sceneMapper.selectOne(new LambdaQueryWrapper<InteractiveScene>().eq(InteractiveScene::getContentId,contentId).eq(InteractiveScene::getIsStart,1).last("LIMIT 1"));if(start==null)throw new BusinessException(ErrorCode.PARAM_ERROR,"影游未配置开始场景");InteractiveProgress p=getOrCreate(contentId,userId);p.setCurrentSceneId(start.getId());p.setCurrentNodeId(null);p.setLastOptionId(null);p.setProgressSeconds(0);p.setIsFinished(0);p.setEndingSceneId(null);p.setLastPlayedAt(LocalDateTime.now());progressMapper.updateById(p);}
    @Override public List<InteractiveScene> endings(Long contentId,Long userId){if(userId==null)throw new BusinessException(ErrorCode.TOKEN_INVALID);Set<Long> candidates=choiceMapper.selectList(new LambdaQueryWrapper<InteractiveChoice>().eq(InteractiveChoice::getUserId,userId).eq(InteractiveChoice::getContentId,contentId).isNotNull(InteractiveChoice::getNextSceneId)).stream().map(InteractiveChoice::getNextSceneId).collect(Collectors.toSet());if(candidates.isEmpty())return List.of();return sceneMapper.selectList(new LambdaQueryWrapper<InteractiveScene>().in(InteractiveScene::getId,candidates).eq(InteractiveScene::getContentId,contentId).eq(InteractiveScene::getSceneType,"ENDING"));}
    private Content requireOnlineInteractive(Long id){Content c=contentMapper.selectById(id);if(c==null||!Integer.valueOf(2).equals(c.getType()))throw new BusinessException(ErrorCode.CONTENT_NOT_FOUND);if(!Integer.valueOf(1).equals(c.getStatus()))throw new BusinessException(ErrorCode.CONTENT_OFFLINE);return c;}
    private InteractiveScene requireScene(Long id){InteractiveScene s=sceneMapper.selectById(id);if(s==null||!Integer.valueOf(1).equals(s.getStatus()))throw new BusinessException(ErrorCode.PARAM_ERROR,"影游场景不存在或未启用");return s;}
    private InteractiveProgress getOrCreate(Long contentId,Long userId){InteractiveProgress p=progressMapper.selectOne(new LambdaQueryWrapper<InteractiveProgress>().eq(InteractiveProgress::getUserId,userId).eq(InteractiveProgress::getContentId,contentId));if(p!=null)return p;InteractiveScene start=sceneMapper.selectOne(new LambdaQueryWrapper<InteractiveScene>().eq(InteractiveScene::getContentId,contentId).eq(InteractiveScene::getIsStart,1).eq(InteractiveScene::getStatus,1).last("LIMIT 1"));p=new InteractiveProgress();p.setUserId(userId);p.setContentId(contentId);p.setCurrentSceneId(start==null?null:start.getId());p.setProgressSeconds(0);p.setIsFinished(0);p.setLastPlayedAt(LocalDateTime.now());progressMapper.insert(p);return p;}
}
