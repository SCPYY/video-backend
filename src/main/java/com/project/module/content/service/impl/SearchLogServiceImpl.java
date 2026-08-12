package com.project.module.content.service.impl;

import com.project.module.content.dto.HotTagVO;
import com.project.module.content.dto.HotContentVO;
import com.project.module.content.entity.Episode;
import com.project.module.content.entity.Content;
import com.project.module.content.entity.SearchLog;
import com.project.module.content.mapper.SearchLogMapper;
import com.project.module.content.mapper.EpisodeMapper;
import com.project.module.content.mapper.ContentMapper;
import com.project.module.content.service.SearchLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Collections;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchLogServiceImpl implements SearchLogService {

    private final SearchLogMapper searchLogMapper;
    private final EpisodeMapper episodeMapper;
    private final ContentMapper contentMapper;

    @Override
    @Transactional
    public void record(Long userId, String keyword, long resultCount, HttpServletRequest request) {
        if (keyword == null || keyword.isBlank()) return;
        String displayKeyword = keyword.trim();
        if (displayKeyword.length() > 100) displayKeyword = displayKeyword.substring(0, 100);

        SearchLog log = new SearchLog();
        log.setUserId(userId);
        log.setKeyword(displayKeyword);
        log.setNormalizedKeyword(displayKeyword.toLowerCase(Locale.ROOT));
        log.setResultCount((int) Math.min(resultCount, Integer.MAX_VALUE));
        log.setIpAddress(clientIp(request));
        String userAgent = request.getHeader("User-Agent");
        log.setUserAgent(userAgent != null && userAgent.length() > 500 ? userAgent.substring(0, 500) : userAgent);
        searchLogMapper.insert(log);
    }

    @Override
    public List<HotTagVO> hotTags(Integer limit, Integer days) {
        int safeLimit = safeLimit(limit, 50);
        List<HotTagVO> result = searchLogMapper.selectHotTags(safeDays(days), safeLimit);
        if (!result.isEmpty()) return result;
        return contentMapper.selectList(new LambdaQueryWrapper<Content>()
                        .eq(Content::getStatus, 1)
                        .isNotNull(Content::getTags)
                        .ne(Content::getTags, "")
                        .orderByDesc(Content::getViewCount)
                        .last("LIMIT " + safeLimit))
                .stream()
                .flatMap(c -> java.util.Arrays.stream(c.getTags().split(",")))
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .distinct()
                .limit(safeLimit)
                .map(tag -> { HotTagVO vo = new HotTagVO(); vo.setTag(tag); vo.setSearchCount(0L); return vo; })
                .collect(Collectors.toList());
    }

    @Override
    public List<HotContentVO> hotContents(Integer limit) {
        List<Content> contents = contentMapper.selectList(new LambdaQueryWrapper<Content>()
                .eq(Content::getStatus, 1)
                .orderByDesc(Content::getViewCount)
                .orderByDesc(Content::getSortOrder)
                .orderByDesc(Content::getId)
                .last("LIMIT " + safeLimit(limit, 20)));
        List<HotContentVO> result = contents.stream().map(content -> {
            HotContentVO vo = new HotContentVO();
            org.springframework.beans.BeanUtils.copyProperties(content, vo);
            return vo;
        }).collect(Collectors.toList());
        List<Long> contentIds = result.stream().map(HotContentVO::getId).collect(Collectors.toList());
        Map<Long, List<Episode>> episodeMap = contentIds.isEmpty() ? Collections.emptyMap() :
                episodeMapper.selectList(new LambdaQueryWrapper<Episode>()
                                .in(Episode::getContentId, contentIds))
                        .stream().collect(Collectors.groupingBy(Episode::getContentId));
        for (int i = 0; i < result.size(); i++) {
            HotContentVO content = result.get(i);
            List<Episode> episodes = episodeMap.getOrDefault(content.getId(), Collections.emptyList());
            content.setRank(i + 1);
            content.setTotalEpisodes(episodes.size());
            content.setFreeEpisodes((int) episodes.stream()
                    .filter(episode -> Integer.valueOf(1).equals(episode.getIsFree())).count());
        }
        return result;
    }

    private int safeLimit(Integer limit, int max) {
        return limit == null ? 10 : Math.max(1, Math.min(limit, max));
    }

    private int safeDays(Integer days) {
        return days == null ? 7 : Math.max(1, Math.min(days, 90));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
