package com.project.module.entitlement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.project.common.exception.BusinessException;
import com.project.common.exception.ErrorCode;
import com.project.module.content.entity.Content;
import com.project.module.content.entity.Episode;
import com.project.module.content.mapper.ContentMapper;
import com.project.module.content.mapper.EpisodeMapper;
import com.project.module.entitlement.dto.EntitlementVO;
import com.project.module.entitlement.entity.UserEntitlement;
import com.project.module.entitlement.mapper.EntitlementMapper;
import com.project.module.entitlement.service.EntitlementService;
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
    @Transactional
    public void grant(Long userId, Long productId) {
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
    }
}
