package com.project.module.social.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.project.common.exception.BusinessException;
import com.project.common.exception.ErrorCode;
import com.project.module.social.dto.DanmakuRequest;
import com.project.module.social.dto.DanmakuVO;
import com.project.module.social.entity.Danmaku;
import com.project.module.social.entity.DanmakuLike;
import com.project.module.social.mapper.DanmakuLikeMapper;
import com.project.module.social.mapper.DanmakuMapper;
import com.project.module.social.service.DanmakuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DanmakuServiceImpl extends ServiceImpl<DanmakuMapper, Danmaku> implements DanmakuService {

    private final DanmakuMapper danmakuMapper;
    private final DanmakuLikeMapper danmakuLikeMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String DANMAKU_RATE_LIMIT_PREFIX = "danmaku:send:";
    private static final int DANMAKU_RATE_LIMIT_COUNT = 3;
    private static final String DANMAKU_CACHE_PREFIX = "danmaku:cache:";

    @Override
    @Transactional
    public DanmakuVO sendDanmaku(DanmakuRequest request, Long userId) {
        // 频率限制：每10秒最多3条
        String rateKey = DANMAKU_RATE_LIMIT_PREFIX + userId + ":" + request.getEpisodeId();
        Long count = stringRedisTemplate.opsForValue().increment(rateKey);
        if (count != null && count == 1) {
            stringRedisTemplate.expire(rateKey, 10, TimeUnit.SECONDS);
        }
        if (count != null && count > DANMAKU_RATE_LIMIT_COUNT) {
            throw new BusinessException(ErrorCode.DANMAKU_RATE_LIMIT);
        }

        Danmaku danmaku = new Danmaku();
        danmaku.setEpisodeId(request.getEpisodeId());
        danmaku.setUserId(userId);
        danmaku.setContent(request.getContent());
        danmaku.setVideoTime(request.getVideoTime());
        danmaku.setColor(request.getColor() != null ? request.getColor() : "#FFFFFF");
        danmaku.setPosition(request.getPosition() != null ? request.getPosition() : "scroll");
        danmaku.setLikeCount(0);
        danmaku.setStatus(1);
        danmakuMapper.insert(danmaku);

        log.info("弹幕发送成功: id={}, episodeId={}, videoTime={}s", danmaku.getId(),
                danmaku.getEpisodeId(), danmaku.getVideoTime());

        return convertToVO(danmaku, Collections.emptySet());
    }

    @Override
    public List<DanmakuVO> getDanmakuList(Long episodeId, Long userId, Integer startTime, Integer endTime) {
        // 缓存Key: danmaku:{episodeId}:{startTime}:{endTime}
        String cacheKey = DANMAKU_CACHE_PREFIX + episodeId + ":" + startTime + ":" + endTime;

        // 查询DB
        List<Danmaku> danmakuList = danmakuMapper.selectByTimeRange(episodeId, startTime, endTime);

        // 查询当前用户对这些弹幕的点赞状态
        Set<Long> likedIds = Collections.emptySet();
        if (userId != null && !danmakuList.isEmpty()) {
            List<Long> danmakuIds = danmakuList.stream().map(Danmaku::getId).collect(Collectors.toList());
            List<DanmakuLike> likes = danmakuLikeMapper.selectByUserAndDanmakuIds(userId, danmakuIds);
            likedIds = likes.stream().map(DanmakuLike::getDanmakuId).collect(Collectors.toSet());
        }

        final Set<Long> finalLikedIds = likedIds;
        List<DanmakuVO> result = danmakuList.stream()
                .map(d -> convertToVO(d, finalLikedIds))
                .collect(Collectors.toList());

        // 写入Redis缓存（30秒）
        try {
            stringRedisTemplate.opsForValue().set(cacheKey,
                    result.size() > 0 ? result.size() + "" : "0", 30, TimeUnit.SECONDS);
        } catch (Exception ignored) {
            // 缓存写入失败不影响返回
        }

        return result;
    }

    @Override
    @Transactional
    public boolean toggleLike(Long id, Long userId) {
        Danmaku danmaku = danmakuMapper.selectById(id);
        if (danmaku == null || danmaku.getStatus() != 1) {
            throw new BusinessException(ErrorCode.DANMAKU_NOT_FOUND);
        }

        DanmakuLike existing = danmakuLikeMapper.selectByDanmakuAndUser(id, userId);
        if (existing != null) {
            // 已点赞 → 取消点赞
            danmakuLikeMapper.deleteById(existing.getId());
            danmaku.setLikeCount(Math.max(0, danmaku.getLikeCount() - 1));
            danmakuMapper.updateById(danmaku);
            return false;
        } else {
            // 未点赞 → 点赞
            DanmakuLike like = new DanmakuLike();
            like.setDanmakuId(id);
            like.setUserId(userId);
            danmakuLikeMapper.insert(like);
            danmakuMapper.incrementLikeCount(id);
            return true;
        }
    }

    @Override
    @Transactional
    public void deleteDanmaku(Long id, Long userId, boolean isAdmin) {
        Danmaku danmaku = danmakuMapper.selectById(id);
        if (danmaku == null || danmaku.getStatus() != 1) {
            throw new BusinessException(ErrorCode.DANMAKU_NOT_FOUND);
        }
        if (!isAdmin && !danmaku.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ADMIN_FORBIDDEN);
        }

        danmaku.setStatus(2);
        danmakuMapper.updateById(danmaku);
        log.info("弹幕删除成功: id={}, userId={}, isAdmin={}", id, userId, isAdmin);
    }

    private DanmakuVO convertToVO(Danmaku d, Set<Long> likedIds) {
        DanmakuVO vo = new DanmakuVO();
        BeanUtils.copyProperties(d, vo);
        vo.setLiked(likedIds.contains(d.getId()));
        return vo;
    }
}
