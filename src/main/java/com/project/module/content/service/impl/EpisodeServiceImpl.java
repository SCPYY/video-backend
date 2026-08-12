package com.project.module.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.project.common.exception.BusinessException;
import com.project.common.exception.ErrorCode;
import com.project.module.content.dto.EpisodePlayVO;
import com.project.module.content.dto.EpisodeVO;
import com.project.module.content.entity.Content;
import com.project.module.content.entity.Episode;
import com.project.module.content.mapper.ContentMapper;
import com.project.module.content.mapper.EpisodeMapper;
import com.project.module.content.service.EpisodeService;
import com.project.module.entitlement.entity.UserEntitlement;
import com.project.module.entitlement.mapper.EntitlementMapper;
import com.project.module.product.entity.Product;
import com.project.module.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EpisodeServiceImpl extends ServiceImpl<EpisodeMapper, Episode> implements EpisodeService {

    private final ContentMapper contentMapper;
    private final EpisodeMapper episodeMapper;
    private final EntitlementMapper entitlementMapper;
    private final ProductMapper productMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public List<EpisodeVO> getEpisodesByContentId(Long contentId) {
        List<Episode> episodes = list(new LambdaQueryWrapper<Episode>()
                .eq(Episode::getContentId, contentId)
                .orderByAsc(Episode::getSortOrder)
                .orderByAsc(Episode::getEpisodeNumber));

        return episodes.stream().map(e -> {
            EpisodeVO vo = new EpisodeVO();
            vo.setId(e.getId());
            vo.setEpisodeNumber(e.getEpisodeNumber());
            vo.setTitle(e.getTitle());
            vo.setDuration(e.getDuration());
            vo.setIsFree(e.getIsFree());
            vo.setSortOrder(e.getSortOrder());
            // 影游互动配置
            if (e.getInteractiveConfig() != null) {
                vo.setInteractiveConfig(e.getInteractiveConfig());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public EpisodePlayVO getPlayInfo(Long episodeId, Long userId, String visitorKey) {
        Episode episode = getById(episodeId);
        if (episode == null) {
            throw new BusinessException(ErrorCode.EPISODE_NOT_FOUND);
        }

        Content content = contentMapper.selectById(episode.getContentId());
        if (content == null || content.getStatus() == 0) {
            throw new BusinessException(ErrorCode.CONTENT_OFFLINE);
        }

        EpisodePlayVO vo = new EpisodePlayVO();
        vo.setId(episode.getId());
        vo.setContentId(episode.getContentId());
        vo.setEpisodeNumber(episode.getEpisodeNumber());
        vo.setTitle(episode.getTitle());
        vo.setDuration(episode.getDuration());
        vo.setIsFree(episode.getIsFree());

        // 解析互动配置（影游）
        if (episode.getInteractiveConfig() != null) {
            vo.setInteractiveConfig(episode.getInteractiveConfig());
        }

        // 判断访问权限
        boolean hasAccess = checkAccess(userId, episode, content);
        vo.setHasAccess(hasAccess);

        if (hasAccess) {
            vo.setVideoUrl(episode.getVideoUrl());
            episodeMapper.incrementPlayCount(episode.getId());
            contentMapper.incrementPlayCount(content.getId());
            String identity = userId != null ? "user:" + userId : "guest:" + Integer.toHexString(visitorKey.hashCode());
            String uniqueKey = "content:play-user:dedup:" + content.getId() + ":" + identity;
            try {
                Boolean first = stringRedisTemplate.opsForValue().setIfAbsent(uniqueKey, "1", java.time.Duration.ofMinutes(30));
                if (Boolean.TRUE.equals(first)) stringRedisTemplate.opsForHash().increment("content:play-user:pending", String.valueOf(content.getId()), 1L);
            } catch (Exception e) { log.warn("记录播放用户数失败: contentId={}", content.getId(), e); }
        } else {
            // 查找推荐的购买商品
            Product suggested = findSuggestedProduct(episode, content);
            if (suggested != null) {
                vo.setSuggestedProductId(suggested.getId());
            }
        }

        return vo;
    }

    /** 检查用户是否有播放权限 */
    private boolean checkAccess(Long userId, Episode episode, Content content) {
        // 免费内容直接放行
        if (episode.getIsFree() == 1) {
            return true;
        }

        // 未登录用户，付费内容无权限
        if (userId == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();

        // 检查是否有该内容的解锁权益（type=1, content_id=该内容ID）
        Long contentEntitlement = entitlementMapper.selectCount(new LambdaQueryWrapper<UserEntitlement>()
                .eq(UserEntitlement::getUserId, userId)
                .eq(UserEntitlement::getType, 1)
                .eq(UserEntitlement::getContentId, content.getId())
                .and(w -> w.isNull(UserEntitlement::getExpireTime)
                        .or().gt(UserEntitlement::getExpireTime, now)));
        if (contentEntitlement > 0) {
            return true;
        }

        // 检查是否有该单集的解锁权益（type=1, episode_id=该剧集ID）
        Long episodeEntitlement = entitlementMapper.selectCount(new LambdaQueryWrapper<UserEntitlement>()
                .eq(UserEntitlement::getUserId, userId)
                .eq(UserEntitlement::getType, 1)
                .eq(UserEntitlement::getEpisodeId, episode.getId())
                .and(w -> w.isNull(UserEntitlement::getExpireTime)
                        .or().gt(UserEntitlement::getExpireTime, now)));
        if (episodeEntitlement > 0) {
            return true;
        }

        // 检查是否有会员权益（type=2，未过期）
        Long memberEntitlement = entitlementMapper.selectCount(new LambdaQueryWrapper<UserEntitlement>()
                .eq(UserEntitlement::getUserId, userId)
                .eq(UserEntitlement::getType, 2)
                .and(w -> w.isNull(UserEntitlement::getExpireTime)
                        .or().gt(UserEntitlement::getExpireTime, now)));
        return memberEntitlement > 0;
    }

    /** 查找推荐商品：优先单集解锁，其次全集解锁 */
    private Product findSuggestedProduct(Episode episode, Content content) {
        // 单集解锁
        Product single = productMapper.selectOne(new LambdaQueryWrapper<Product>()
                .eq(Product::getType, 1)
                .eq(Product::getEpisodeId, episode.getId())
                .eq(Product::getStatus, 1));
        if (single != null) return single;

        // 全集解锁
        Product full = productMapper.selectOne(new LambdaQueryWrapper<Product>()
                .eq(Product::getType, 2)
                .eq(Product::getContentId, content.getId())
                .eq(Product::getStatus, 1));
        if (full != null) return full;

        // 会员
        return productMapper.selectOne(new LambdaQueryWrapper<Product>()
                .eq(Product::getType, 3)
                .eq(Product::getStatus, 1));
    }
}
