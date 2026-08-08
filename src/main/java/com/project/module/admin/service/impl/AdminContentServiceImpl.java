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
import com.project.module.content.mapper.ContentExtrasMapper;
import com.project.module.content.mapper.ContentMapper;
import com.project.module.content.mapper.EpisodeMapper;
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

    private static final String IDEMPOTENT_PREFIX = "admin:content:idempotent:";
    private static final long IDEMPOTENT_TTL = 24;

    @Override
    public Page<Content> pageContents(Integer type, Integer status, Integer pageNum, Integer size) {
        LambdaQueryWrapper<Content> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(type != null, Content::getType, type);
        wrapper.eq(status != null, Content::getStatus, status);
        wrapper.ne(Content::getStatus, -1); // 排除已删除
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
        titleWrapper.ne(Content::getStatus, -1);
        if (contentMapper.selectCount(titleWrapper) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "标题已存在");
        }

        Content content = new Content();
        BeanUtils.copyProperties(req, content, "extras");
        content.setViewCount(0L);
        content.setCreatedBy(adminId);
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

        // 标题重复检查（排除自身）
        LambdaQueryWrapper<Content> titleWrapper = new LambdaQueryWrapper<>();
        titleWrapper.eq(Content::getTitle, req.getTitle());
        titleWrapper.ne(Content::getId, id);
        titleWrapper.ne(Content::getStatus, -1);
        if (contentMapper.selectCount(titleWrapper) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "标题已存在");
        }

        // 记录变更前快照
        String beforeJson = toJson(existing);

        BeanUtils.copyProperties(req, existing, "extras");
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
        Content content = contentMapper.selectById(id);
        if (content == null || content.getStatus() == -1) {
            throw new BusinessException(ErrorCode.CONTENT_NOT_FOUND);
        }

        int oldStatus = content.getStatus();
        // 状态流转：0→1(上架), 1→0(下架), 2→0(违规→下架)
        int newStatus;
        switch (oldStatus) {
            case 0:
                newStatus = 1;
                break;
            case 1:
                newStatus = 0;
                break;
            case 2:
                newStatus = 0;
                break;
            default:
                throw new BusinessException(ErrorCode.PARAM_ERROR, "当前状态不允许切换");
        }

        String beforeJson = toJson(content);
        content.setStatus(newStatus);
        content.setUpdatedBy(adminId);
        contentMapper.updateById(content);

        // 审计日志
        adminLogService.log(adminId, "UPDATE", "CONTENT",
                String.valueOf(id),
                Map.of("beforeStatus", oldStatus),
                Map.of("afterStatus", newStatus));

        log.info("内容状态切换: id={}, {} -> {}, adminId={}", id, oldStatus, newStatus, adminId);
    }

    private void saveExtras(Long contentId, Map<String, Object> extras) {
        extras.forEach((key, value) -> {
            ContentExtras extra = new ContentExtras();
            extra.setContentId(contentId);
            extra.setKey(key);
            extra.setValue(value != null ? value.toString() : null);
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
