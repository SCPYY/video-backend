package com.project.module.admin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.project.common.exception.BusinessException;
import com.project.common.exception.ErrorCode;
import com.project.module.admin.dto.ContentCreateRequest;
import com.project.module.admin.service.AdminContentService;
import com.project.module.admin.service.AdminLogService;
import com.project.module.content.entity.Content;
import com.project.module.content.entity.ContentExtras;
import com.project.module.content.entity.Episode;
import com.project.module.content.dto.ContentDetailVO;
import com.project.module.content.mapper.ContentExtrasMapper;
import com.project.module.content.mapper.ContentMapper;
import com.project.module.content.mapper.EpisodeMapper;
import com.project.module.content.entity.ContentStatusLog;
import com.project.module.content.mapper.ContentStatusLogMapper;
import com.project.module.content.entity.InteractiveScene;
import com.project.module.content.mapper.InteractiveSceneMapper;
import com.project.module.content.entity.InteractiveNode;
import com.project.module.content.entity.InteractiveOption;
import com.project.module.content.mapper.InteractiveNodeMapper;
import com.project.module.content.mapper.InteractiveOptionMapper;
import com.project.module.message.service.MessageService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminContentServiceImpl implements AdminContentService {

    private final ContentMapper contentMapper;
    private final ContentExtrasMapper contentExtrasMapper;
    private final EpisodeMapper episodeMapper;
    private final AdminLogService adminLogService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ContentStatusLogMapper statusLogMapper;
    private final InteractiveSceneMapper sceneMapper;
    private final InteractiveNodeMapper nodeMapper;
    private final InteractiveOptionMapper optionMapper;
    private final MessageService messageService;

    private static final int DRAFT=0, PENDING=1, REVIEWING=2, APPROVED=3, PUBLISHED=4, OFFLINE=5, DELETED=6, REJECTED=7;

    @Override
    public ContentDetailVO getAdminDetail(Long id) {
        Content content = contentMapper.selectById(id);
        if (content == null || content.getStatus() == -1) {
            throw new BusinessException(ErrorCode.CONTENT_NOT_FOUND);
        }
        ContentDetailVO vo = new ContentDetailVO();
        BeanUtils.copyProperties(content, vo);
        Map<String, Object> extras = new java.util.HashMap<>();
        contentExtrasMapper.selectList(new LambdaQueryWrapper<ContentExtras>()
                        .eq(ContentExtras::getContentId, id))
                .forEach(item -> extras.put(item.getKey(), item.getValue()));
        vo.setExtras(extras);
        vo.setTotalEpisodes(Math.toIntExact(episodeMapper.selectCount(
                new LambdaQueryWrapper<Episode>().eq(Episode::getContentId, id))));
        return vo;
    }

    private static final String IDEMPOTENT_PREFIX = "admin:content:idempotent:";
    private static final long IDEMPOTENT_TTL = 24;

    @Override
    public Page<Content> pageContents(Integer type, Integer status, Integer pageNum, Integer size) {
        LambdaQueryWrapper<Content> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(type != null, Content::getType, type);
        wrapper.eq(status != null, Content::getContentStatus, status);
        wrapper.ne(Content::getContentStatus, 6); // 排除已删除
        wrapper.orderByDesc(Content::getSortOrder);
        wrapper.orderByDesc(Content::getId);
        return contentMapper.selectPage(
                new Page<>(pageNum != null ? pageNum : 1, size != null ? size : 10), wrapper);
    }

    @Override
    @Transactional
    public Content createContent(ContentCreateRequest req, Long adminId) {
        // 1. 幂等检查
        if (req.getIdempotentKey() != null && !req.getIdempotentKey().isBlank()) {
            String key = IDEMPOTENT_PREFIX + req.getIdempotentKey();
            Boolean locked = stringRedisTemplate.opsForValue()
                    .setIfAbsent(key, "1", IDEMPOTENT_TTL, TimeUnit.HOURS);
            if (Boolean.FALSE.equals(locked)) {
                throw new BusinessException(ErrorCode.DUPLICATE_ORDER, "请勿重复提交");
            }
        }

        // 2. 标题重复检查
        LambdaQueryWrapper<Content> titleWrapper = new LambdaQueryWrapper<>();
        titleWrapper.eq(Content::getTitle, req.getTitle());
        titleWrapper.ne(Content::getContentStatus, 6);
        if (contentMapper.selectCount(titleWrapper) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "标题已存在");
        }

        Content content = new Content();
        BeanUtils.copyProperties(req, content, "extras", "status");
        content.setViewCount(0L);
        int onlineStatus = Integer.valueOf(1).equals(req.getStatus()) ? 1 : 0;
        content.setContentStatus(onlineStatus == 1 ? PUBLISHED : DRAFT);
        content.setStatus(onlineStatus);
        content.setCreatedBy(adminId);
        content.setCategoryId(req.getCategoryId());
        contentMapper.insert(content);

        // 保存扩展属性
        if (req.getExtras() != null && !req.getExtras().isEmpty()) {
            saveExtras(content.getId(), req.getExtras());
        }

        // 审计日志
        adminLogService.log(adminId, "CREATE", "CONTENT",
                String.valueOf(content.getId()), null, content);

        log.info("内容创建成功: id={}, title={}, adminId={}", content.getId(), content.getTitle(), adminId);
        return content;
    }

    @Override
    @Transactional
    public Content updateContent(Long id, ContentCreateRequest req, Long adminId) {
        Content existing = contentMapper.selectById(id);
        if (existing == null || existing.getStatus() == -1) {
            throw new BusinessException(ErrorCode.CONTENT_NOT_FOUND);
        }
        if (!(existing.getContentStatus() == null || existing.getContentStatus() == DRAFT
                || existing.getContentStatus() == REJECTED || existing.getContentStatus() == OFFLINE)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "当前状态不允许编辑");
        }

        // 标题重复检查（排除自身）
        LambdaQueryWrapper<Content> titleWrapper = new LambdaQueryWrapper<>();
        titleWrapper.eq(Content::getTitle, req.getTitle());
        titleWrapper.ne(Content::getId, id);
        titleWrapper.ne(Content::getContentStatus, 6);
        if (contentMapper.selectCount(titleWrapper) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "标题已存在");
        }

        // 记录变更前快照
        String beforeJson = toJson(existing);

        BeanUtils.copyProperties(req, existing, "extras", "contentStatus", "rejectReason");
        int onlineStatus = Integer.valueOf(1).equals(req.getStatus()) ? 1 : 0;
        existing.setStatus(onlineStatus);
        existing.setContentStatus(onlineStatus == 1 ? PUBLISHED : DRAFT);
        existing.setCategoryId(req.getCategoryId());
        existing.setId(id);
        existing.setUpdatedBy(adminId);
        contentMapper.updateById(existing);

        // 更新扩展属性（先删后增）
        if (req.getExtras() != null) {
            contentExtrasMapper.delete(new LambdaQueryWrapper<ContentExtras>()
                    .eq(ContentExtras::getContentId, id));
            if (!req.getExtras().isEmpty()) {
                saveExtras(id, req.getExtras());
            }
        }

        // 审计日志
        Content updated = contentMapper.selectById(id);
        adminLogService.log(adminId, "UPDATE", "CONTENT", String.valueOf(id), beforeJson, updated);

        log.info("内容更新成功: id={}, adminId={}", id, adminId);
        return updated;
    }

    @Override
    @Transactional
    public void deleteContent(Long id, Long adminId) {
        Content content = contentMapper.selectById(id);
        if (content == null || content.getStatus() == -1) {
            throw new BusinessException(ErrorCode.CONTENT_NOT_FOUND);
        }

        // 软删除：status = -1
        String beforeJson = toJson(content);
        transition(content, DELETED, "DELETE", null, adminId);
        content.setStatus(-1);
        content.setUpdatedBy(adminId);
        contentMapper.updateById(content);

        // 软删除关联剧集
        episodeMapper.softDeleteByContentId(id);

        // 审计日志
        adminLogService.log(adminId, "DELETE", "CONTENT", String.valueOf(id), beforeJson, null);

        log.info("内容软删除成功: id={}, title={}, adminId={}", id, content.getTitle(), adminId);
    }

    @Override
    @Transactional
    public void toggleStatus(Long id, Long adminId) {
        Content c = requireContent(id);
        if (c.getStatus() != null && c.getStatus() == 1) offline(id, adminId);
        else publish(id, adminId);
    }

    @Override @Transactional public void submitReview(Long id, Long adminId) { transitionBy(id, PENDING, "SUBMIT_REVIEW", null, adminId, DRAFT); messageService.send("ADMIN", null, "CONTENT_REVIEW", "REVIEW_CONTENT", "有新的内容待审核", "内容ID："+id+" 已提交审核", "CONTENT", id, "/content/review?contentId="+id, "CONTENT", id); }
    @Override @Transactional public void withdrawReview(Long id, Long adminId) { transitionBy(id, DRAFT, "WITHDRAW_REVIEW", null, adminId, PENDING); }
    @Override @Transactional public void startReview(Long id, Long adminId) { transitionBy(id, REVIEWING, "START_REVIEW", null, adminId, PENDING); }
    @Override @Transactional public void approve(Long id, Long adminId) { transitionBy(id, APPROVED, "APPROVE", null, adminId, PENDING, REVIEWING); }
    @Override @Transactional public void reject(Long id, Long adminId, String reason) { transitionBy(id, DRAFT, "REJECT", reason, adminId, PENDING, REVIEWING); }
    @Override @Transactional public void publish(Long id, Long adminId) {
        Content c = requireContent(id); if (c.getContentStatus() == DELETED) throw new BusinessException(ErrorCode.PARAM_ERROR,"内容已删除，不能上架");
        validateBeforePublish(c);
        transition(c,PUBLISHED,"PUBLISH",null,adminId); c.setStatus(1); c.setPublishedAt(LocalDateTime.now()); c.setPublishedBy(adminId); contentMapper.updateById(c);
    }
    @Override @Transactional public void offline(Long id, Long adminId) {
        Content c = requireContent(id); if (c.getStatus() == null || c.getStatus() != 1) throw new BusinessException(ErrorCode.PARAM_ERROR,"当前内容未上架");
        transition(c,OFFLINE,"OFFLINE",null,adminId); c.setStatus(0); c.setOfflineAt(LocalDateTime.now()); c.setOfflineBy(adminId); contentMapper.updateById(c);
    }
    @Override public List<ContentStatusLog> statusLogs(Long id) { requireContent(id); return statusLogMapper.selectList(new LambdaQueryWrapper<ContentStatusLog>().eq(ContentStatusLog::getContentId,id).orderByDesc(ContentStatusLog::getId)); }

    private void transitionBy(Long id,int target,String action,String reason,Long adminId,int... allowed) { Content c=requireContent(id); for(int s:allowed) if(c.getContentStatus()!=null && c.getContentStatus()==s){ if(target==REVIEWING || target==APPROVED) ensureReviewer(c,adminId); transition(c,target,action,reason,adminId); return; } throw new BusinessException(ErrorCode.PARAM_ERROR,"当前状态不允许执行该操作"); }
    private void ensureReviewer(Content c, Long adminId) { if (adminId != null && (adminId.equals(c.getCreatedBy()) || adminId.equals(c.getUpdatedBy()))) throw new BusinessException(ErrorCode.PARAM_ERROR,"内容创建人或最后编辑人不能审核自己的内容"); }
    private void validateBeforePublish(Content c) {
        if (c.getTitle()==null || c.getTitle().isBlank() || c.getCoverUrl()==null || c.getCoverUrl().isBlank()) throw new BusinessException(ErrorCode.PARAM_ERROR,"标题和封面不能为空");
        if (c.getType()==2) {
            List<InteractiveScene> scenes=sceneMapper.selectList(new LambdaQueryWrapper<InteractiveScene>().eq(InteractiveScene::getContentId,c.getId()));
            if (scenes.isEmpty()) throw new BusinessException(ErrorCode.PARAM_ERROR,"影游至少需要一个场景");
            if (scenes.stream().filter(s->Integer.valueOf(1).equals(s.getIsStart())).count()!=1) throw new BusinessException(ErrorCode.PARAM_ERROR,"影游必须且只能有一个开始场景");
            if (scenes.stream().noneMatch(s->"ENDING".equalsIgnoreCase(s.getSceneType()))) throw new BusinessException(ErrorCode.PARAM_ERROR,"影游必须配置结局场景");
            var sceneIds=scenes.stream().map(InteractiveScene::getId).collect(java.util.stream.Collectors.toSet());
            var nodes=nodeMapper.selectList(new LambdaQueryWrapper<InteractiveNode>().in(InteractiveNode::getSceneId,sceneIds)); var nodeIds=nodes.stream().map(InteractiveNode::getId).collect(java.util.stream.Collectors.toSet());
            var options=nodeIds.isEmpty()?List.<InteractiveOption>of():optionMapper.selectList(new LambdaQueryWrapper<InteractiveOption>().in(InteractiveOption::getNodeId,nodeIds));
            for(InteractiveOption o:options) if((o.getNextSceneId()==null||!sceneIds.contains(o.getNextSceneId()))&&(o.getNextNodeId()==null||!nodeIds.contains(o.getNextNodeId()))) throw new BusinessException(ErrorCode.PARAM_ERROR,"存在无效的影游选项跳转");
        }
    }
    private Content requireContent(Long id) { Content c=contentMapper.selectById(id); if(c==null || c.getStatus()==-1 || c.getContentStatus()==DELETED) throw new BusinessException(ErrorCode.CONTENT_NOT_FOUND); return c; }
    private void transition(Content c,int target,String action,String reason,Long adminId) { int from=c.getContentStatus()==null?DRAFT:c.getContentStatus(); c.setContentStatus(target); if ("REJECT".equals(action)) c.setRejectReason(reason); else if (target==PENDING || target==APPROVED) c.setRejectReason(null); c.setUpdatedBy(adminId); if(target==PENDING)c.setSubmittedAt(LocalDateTime.now()); if(target==REVIEWING)c.setReviewStartedAt(LocalDateTime.now()); if(target==APPROVED){c.setReviewedAt(LocalDateTime.now());c.setReviewedBy(adminId);} contentMapper.updateById(c); ContentStatusLog l=new ContentStatusLog(); l.setContentId(c.getId());l.setFromStatus(from);l.setToStatus(target);l.setAction(action);l.setReason(reason);l.setOperatorId(adminId);statusLogMapper.insert(l);adminLogService.log(adminId,action,"CONTENT",String.valueOf(c.getId()),from,target); }

    private void saveExtras(Long contentId, Map<String, Object> extras) {
        extras.forEach((key, value) -> {
            ContentExtras extra = new ContentExtras();
            extra.setContentId(contentId);
            extra.setKey(key);
            try {
                // content_extras.value 是 JSON 字段，必须保存合法 JSON：字符串、数字、布尔值均保留原类型。
                extra.setValue(value == null ? "null" : objectMapper.writeValueAsString(value));
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "扩展属性格式不合法: " + key);
            }
            contentExtrasMapper.insert(extra);
        });
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("JSON序列化失败", e);
            return "{}";
        }
    }
}
