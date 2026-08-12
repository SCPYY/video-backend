package com.project.module.entitlement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.project.common.exception.BusinessException;
import com.project.common.exception.ErrorCode;
import com.project.module.content.entity.Content;
import com.project.module.content.entity.Episode;
import com.project.module.content.mapper.ContentMapper;
import com.project.module.content.mapper.EpisodeMapper;
import com.project.module.entitlement.dto.EntitlementVO;
import com.project.module.entitlement.dto.MyEntitlementVO;
import com.project.module.entitlement.entity.UserEntitlement;
import com.project.module.entitlement.mapper.EntitlementMapper;
import com.project.module.entitlement.service.EntitlementService;
import com.project.module.admin.service.AdminLogService;
import com.project.module.product.entity.Product;
import com.project.module.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntitlementServiceImpl extends ServiceImpl<EntitlementMapper, UserEntitlement> implements EntitlementService {

    private final ContentMapper contentMapper;
    private final EpisodeMapper episodeMapper;
    private final ProductMapper productMapper;
    private final AdminLogService adminLogService;

    @Override
    public boolean checkAccess(Long userId, Long contentId, Long episodeId) {
        if (userId == null) {
            return false;
        }
        if (episodeId != null) {
            Episode episode = episodeMapper.selectById(episodeId);
            if (episode != null && episode.getIsFree() == 1) {
                return true;
            }
        }

        LocalDateTime now = LocalDateTime.now();

        // 内容解锁权益 (type=1)
        LambdaQueryWrapper<UserEntitlement> wrapper = new LambdaQueryWrapper<UserEntitlement>()
                .eq(UserEntitlement::getUserId, userId)
                .eq(UserEntitlement::getType, 1)
                .and(w -> w.isNull(UserEntitlement::getExpireTime)
                        .or().gt(UserEntitlement::getExpireTime, now));

        // 内容级别：content_id匹配
        if (contentId != null) {
            long count = count(wrapper.clone().eq(UserEntitlement::getContentId, contentId));
            if (count > 0) return true;
        }

        // 单集级别：episode_id匹配
        if (episodeId != null) {
            long count = count(wrapper.clone().eq(UserEntitlement::getEpisodeId, episodeId));
            if (count > 0) return true;
        }

        // 会员权益 (type=2)
        long memberCount = count(new LambdaQueryWrapper<UserEntitlement>()
                .eq(UserEntitlement::getUserId, userId)
                .eq(UserEntitlement::getType, 2)
                .and(w -> w.isNull(UserEntitlement::getExpireTime)
                        .or().gt(UserEntitlement::getExpireTime, now)));
        return memberCount > 0;
    }

    @Override
    public List<EntitlementVO> listUserEntitlements(Long userId) {
        List<UserEntitlement> entitlements = list(new LambdaQueryWrapper<UserEntitlement>()
                .eq(UserEntitlement::getUserId, userId)
                .orderByDesc(UserEntitlement::getCreatedAt));

        // 批量获取关联的内容标题和剧集编号
        List<Long> contentIds = entitlements.stream()
                .map(UserEntitlement::getContentId).filter(id -> id != null).distinct().collect(Collectors.toList());
        List<Long> episodeIds = entitlements.stream()
                .map(UserEntitlement::getEpisodeId).filter(id -> id != null).distinct().collect(Collectors.toList());

        Map<Long, String> contentTitleMap = contentIds.isEmpty() ? Collections.emptyMap() :
                contentMapper.selectBatchIds(contentIds).stream()
                        .collect(Collectors.toMap(Content::getId, Content::getTitle));
        Map<Long, Integer> episodeNumMap = episodeIds.isEmpty() ? Collections.emptyMap() :
                episodeMapper.selectBatchIds(episodeIds).stream()
                        .collect(Collectors.toMap(Episode::getId, Episode::getEpisodeNumber));

        LocalDateTime now = LocalDateTime.now();
        return entitlements.stream().map(e -> {
            EntitlementVO vo = new EntitlementVO();
            vo.setId(e.getId());
            vo.setUserId(e.getUserId());
            vo.setType(e.getType());
            vo.setContentId(e.getContentId());
            vo.setContentTitle(e.getContentId() != null ? contentTitleMap.get(e.getContentId()) : null);
            vo.setEpisodeId(e.getEpisodeId());
            vo.setEpisodeNumber(e.getEpisodeId() != null ? episodeNumMap.get(e.getEpisodeId()) : null);
            vo.setExpireTime(e.getExpireTime());
            vo.setIsExpired(e.getExpireTime() != null && e.getExpireTime().isBefore(now));
            vo.setCreatedAt(e.getCreatedAt());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public Page<MyEntitlementVO> pageMyEntitlements(Long userId, Integer contentType,
                                                     Integer pageNum, Integer size) {
        if (contentType != null && contentType != 1 && contentType != 2) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "内容类型只能是1（短剧）或2（影游）");
        }

        int safePage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safeSize = size == null ? 12 : Math.max(1, Math.min(size, 100));
        LambdaQueryWrapper<UserEntitlement> wrapper = new LambdaQueryWrapper<UserEntitlement>()
                .eq(UserEntitlement::getUserId, userId)
                .eq(UserEntitlement::getType, 1)
                .isNotNull(UserEntitlement::getContentId)
                .orderByDesc(UserEntitlement::getCreatedAt)
                .orderByDesc(UserEntitlement::getId);

        if (contentType != null) {
            List<Long> typedContentIds = contentMapper.selectList(new LambdaQueryWrapper<Content>()
                            .select(Content::getId)
                            .eq(Content::getType, contentType))
                    .stream().map(Content::getId).collect(Collectors.toList());
            if (typedContentIds.isEmpty()) {
                return new Page<>(safePage, safeSize, 0);
            }
            wrapper.in(UserEntitlement::getContentId, typedContentIds);
        }

        Page<UserEntitlement> source = page(new Page<>(safePage, safeSize), wrapper);
        List<Long> contentIds = source.getRecords().stream()
                .map(UserEntitlement::getContentId).distinct().collect(Collectors.toList());
        List<Long> episodeIds = source.getRecords().stream()
                .map(UserEntitlement::getEpisodeId).filter(id -> id != null).distinct().collect(Collectors.toList());

        Map<Long, Content> contentMap = contentIds.isEmpty() ? Collections.emptyMap() :
                contentMapper.selectBatchIds(contentIds).stream()
                        .collect(Collectors.toMap(Content::getId, content -> content));
        Map<Long, Episode> episodeMap = episodeIds.isEmpty() ? Collections.emptyMap() :
                episodeMapper.selectBatchIds(episodeIds).stream()
                        .collect(Collectors.toMap(Episode::getId, episode -> episode));

        LocalDateTime now = LocalDateTime.now();
        List<MyEntitlementVO> records = source.getRecords().stream().map(entitlement -> {
            MyEntitlementVO vo = new MyEntitlementVO();
            Content content = contentMap.get(entitlement.getContentId());
            Episode episode = entitlement.getEpisodeId() == null
                    ? null : episodeMap.get(entitlement.getEpisodeId());
            vo.setEntitlementId(entitlement.getId());
            vo.setContentId(entitlement.getContentId());
            if (content != null) {
                vo.setContentType(content.getType());
                vo.setTitle(content.getTitle());
                vo.setDescription(content.getDescription());
                vo.setCoverUrl(content.getCoverUrl());
                vo.setCategory(content.getCategory());
                vo.setTags(content.getTags());
            }
            vo.setAccessScope(entitlement.getEpisodeId() == null ? "FULL_CONTENT" : "SINGLE_EPISODE");
            vo.setEpisodeId(entitlement.getEpisodeId());
            if (episode != null) {
                vo.setEpisodeNumber(episode.getEpisodeNumber());
                vo.setEpisodeTitle(episode.getTitle());
            }
            vo.setExpireTime(entitlement.getExpireTime());
            vo.setPermanent(entitlement.getExpireTime() == null);
            vo.setExpired(entitlement.getExpireTime() != null && !entitlement.getExpireTime().isAfter(now));
            vo.setAcquiredAt(entitlement.getCreatedAt());
            return vo;
        }).collect(Collectors.toList());

        Page<MyEntitlementVO> result = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        result.setPages(source.getPages());
        result.setRecords(records);
        return result;
    }

    @Override
    @Transactional
    public UserEntitlement grant(Long userId, Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null || product.getStatus() == 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "商品不存在或已下架");
        }

        UserEntitlement entitlement = new UserEntitlement();
        entitlement.setUserId(userId);

        switch (product.getType()) {
            case 1: // 单集解锁
                entitlement.setType(1);
                entitlement.setContentId(product.getContentId());
                entitlement.setEpisodeId(product.getEpisodeId());
                // 单集解锁永久有效
                break;
            case 2: // 全集解锁
                entitlement.setType(1);
                entitlement.setContentId(product.getContentId());
                // 全集解锁永久有效
                break;
            case 3: // 会员
                entitlement.setType(2);
                if (product.getDurationDays() != null && product.getDurationDays() > 0) {
                    entitlement.setExpireTime(LocalDateTime.now().plusDays(product.getDurationDays()));
                }
                break;
            default:
                throw new BusinessException(ErrorCode.PARAM_ERROR, "未知商品类型");
        }

        save(entitlement);
        log.info("权益发放成功: userId={}, productId={}, type={}", userId, productId, product.getType());
        return entitlement;
    }

    @Override
    public Page<EntitlementVO> pageAdminEntitlements(Long userId, Integer type, Integer pageNum, Integer size) {
        Page<UserEntitlement> source = page(new Page<>(pageNum == null || pageNum < 1 ? 1 : pageNum,
                        size == null ? 20 : Math.min(100, Math.max(1, size))),
                new LambdaQueryWrapper<UserEntitlement>()
                        .eq(userId != null, UserEntitlement::getUserId, userId)
                        .eq(type != null, UserEntitlement::getType, type)
                        .orderByDesc(UserEntitlement::getId));
        Page<EntitlementVO> result = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        result.setRecords(source.getRecords().stream().map(this::toEntitlementVO).collect(Collectors.toList()));
        return result;
    }

    @Override
    @Transactional
    public EntitlementVO grantByProduct(Long userId, Long productId, Long adminId) {
        UserEntitlement entitlement = grant(userId, productId);
        adminLogService.log(adminId, "GRANT", "ENTITLEMENT", String.valueOf(entitlement.getId()), null, entitlement);
        return toEntitlementVO(entitlement);
    }

    @Override
    @Transactional
    public void revoke(Long entitlementId, Long adminId) {
        UserEntitlement entitlement = getById(entitlementId);
        if (entitlement == null) throw new BusinessException(ErrorCode.PARAM_ERROR, "权益不存在");
        removeById(entitlementId);
        adminLogService.log(adminId, "REVOKE", "ENTITLEMENT", String.valueOf(entitlementId), entitlement, null);
    }

    private EntitlementVO toEntitlementVO(UserEntitlement e) {
        EntitlementVO vo = new EntitlementVO();
        vo.setId(e.getId()); vo.setUserId(e.getUserId()); vo.setType(e.getType());
        vo.setContentId(e.getContentId()); vo.setEpisodeId(e.getEpisodeId());
        vo.setExpireTime(e.getExpireTime()); vo.setIsExpired(e.getExpireTime() != null && e.getExpireTime().isBefore(LocalDateTime.now()));
        vo.setCreatedAt(e.getCreatedAt());
        return vo;
    }
}
