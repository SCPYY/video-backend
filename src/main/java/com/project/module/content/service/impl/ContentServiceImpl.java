package com.project.module.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.project.common.exception.BusinessException;
import com.project.common.exception.ErrorCode;
import com.project.module.content.dto.ContentDetailVO;
import com.project.module.content.dto.ContentListItemVO;
import com.project.module.content.dto.ContentListQuery;
import com.project.module.content.entity.Content;
import com.project.module.content.entity.ContentExtras;
import com.project.module.content.entity.Episode;
import com.project.module.content.mapper.ContentExtrasMapper;
import com.project.module.content.mapper.ContentMapper;
import com.project.module.content.mapper.EpisodeMapper;
import com.project.module.content.service.ContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentServiceImpl extends ServiceImpl<ContentMapper, Content> implements ContentService {

    private final ContentExtrasMapper contentExtrasMapper;
    private final EpisodeMapper episodeMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String CACHE_CONTENT_DETAIL = "content:detail:";
    private static final String CACHE_HOT_CONTENT = "content:hot";
    private static final long CACHE_TTL_MINUTES = 30;

    @Override
    public Page<ContentListItemVO> pageContent(ContentListQuery query) {
        LambdaQueryWrapper<Content> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(query.getType() != null, Content::getType, query.getType());
        wrapper.eq(query.getStatus() != null, Content::getStatus, query.getStatus());
        wrapper.eq(Content::getStatus, 1);
        wrapper.eq(query.getCategory() != null, Content::getCategoryId, query.getCategory());
        wrapper.like(query.getKeyword() != null && !query.getKeyword().isBlank(),
                Content::getTitle, query.getKeyword());
        wrapper.orderByDesc(Content::getSortOrder);
        wrapper.orderByDesc(Content::getId);

        Page<Content> page = page(new Page<>(query.getPage(), query.getSize()), wrapper);

        // 转换为 VO，补充剧集统计
        List<ContentListItemVO> records = page.getRecords().stream().map(c -> {
            ContentListItemVO vo = new ContentListItemVO();
            BeanUtils.copyProperties(c, vo);
            // 统计该内容的剧集数和免费集数
            long total = episodeMapper.selectCount(
                    new LambdaQueryWrapper<Episode>().eq(Episode::getContentId, c.getId()));
            long free = episodeMapper.selectCount(
                    new LambdaQueryWrapper<Episode>()
                            .eq(Episode::getContentId, c.getId())
                            .eq(Episode::getIsFree, 1));
            vo.setTotalEpisodes((int) total);
            vo.setFreeEpisodes((int) free);
            return vo;
        }).collect(Collectors.toList());

        Page<ContentListItemVO> result = new Page<>();
        BeanUtils.copyProperties(page, result, "records");
        result.setRecords(records);
        return result;
    }

    @Override
    public List<ContentListItemVO> carouselContent() {
        LambdaQueryWrapper<Content> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Content::getType, 1);
        wrapper.eq(Content::getContentStatus, 4);
        wrapper.orderByDesc(Content::getViewCount);
        wrapper.last("LIMIT 6");

        List<Content> list = list(wrapper);
        return toVOList(list);
    }

    @Override
    public ContentDetailVO getDetail(Long id, Long userId, String visitorKey) {
        Content content = getById(id);
        if (content == null) {
            throw new BusinessException(ErrorCode.CONTENT_NOT_FOUND);
        }
        if (!Integer.valueOf(4).equals(content.getContentStatus())) {
            throw new BusinessException(ErrorCode.CONTENT_OFFLINE);
        }

        recordView(id, userId, visitorKey);

        ContentDetailVO vo = new ContentDetailVO();
        BeanUtils.copyProperties(content, vo);

        // 查询扩展属性
        List<ContentExtras> extrasList = contentExtrasMapper.selectList(
                new LambdaQueryWrapper<ContentExtras>().eq(ContentExtras::getContentId, id));
        Map<String, Object> extras = new HashMap<>();
        for (ContentExtras e : extrasList) {
            extras.put(e.getKey(), e.getValue());
        }
        vo.setExtras(extras);

        // 统计剧集数
        long total = episodeMapper.selectCount(
                new LambdaQueryWrapper<Episode>().eq(Episode::getContentId, id));
        vo.setTotalEpisodes((int) total);

        // 异步增加观看次数（简单实现：每次查看+1）
        asyncIncrementViewCount(id);

        return vo;
    }

    private void recordView(Long contentId, Long userId, String visitorKey) {
        String identity = userId != null ? "user:" + userId : "guest:" + Integer.toHexString(visitorKey.hashCode());
        String dedupKey = "content:view:dedup:" + contentId + ":" + identity;
        try {
            Boolean first = stringRedisTemplate.opsForValue().setIfAbsent(dedupKey, "1", Duration.ofMinutes(30));
            if (Boolean.TRUE.equals(first)) stringRedisTemplate.opsForHash().increment("content:view:pending", String.valueOf(contentId), 1L);
        } catch (Exception e) { log.warn("记录内容浏览量失败: contentId={}", contentId, e); }
    }

    /** 批量统计剧集后转为 VO 列表 */
    private List<ContentListItemVO> toVOList(List<Content> contentList) {
        if (contentList.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> contentIds = contentList.stream().map(Content::getId).collect(Collectors.toList());
        List<Map<String, Object>> stats = episodeMapper.selectMaps(
                new LambdaQueryWrapper<Episode>().in(Episode::getContentId, contentIds)
                        .select(Episode::getContentId, Episode::getId, Episode::getIsFree));
        Map<Long, Long> totalMap = stats.stream()
                .collect(Collectors.groupingBy(m -> (Long) m.get("content_id"), Collectors.counting()));
        Map<Long, Long> freeMap = stats.stream()
                .filter(m -> Integer.valueOf(1).equals(m.get("is_free")))
                .collect(Collectors.groupingBy(m -> (Long) m.get("content_id"), Collectors.counting()));

        return contentList.stream().map(c -> {
            ContentListItemVO vo = new ContentListItemVO();
            BeanUtils.copyProperties(c, vo);
            vo.setTotalEpisodes(totalMap.getOrDefault(c.getId(), 0L).intValue());
            vo.setFreeEpisodes(freeMap.getOrDefault(c.getId(), 0L).intValue());
            return vo;
        }).collect(Collectors.toList());
    }

    /** 异步更新观看次数，通过 Redis 计数后定时批量同步到 DB */
    private void asyncIncrementViewCount(Long contentId) {
        String key = "content:views:" + contentId;
        stringRedisTemplate.opsForValue().increment(key);
        stringRedisTemplate.expire(key, 1, TimeUnit.HOURS);
    }

}
