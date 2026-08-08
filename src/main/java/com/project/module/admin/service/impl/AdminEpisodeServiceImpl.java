package com.project.module.admin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.exception.BusinessException;
import com.project.common.exception.ErrorCode;
import com.project.module.admin.dto.BatchAddEpisodesRequest;
import com.project.module.admin.dto.SortEpisodesRequest;
import com.project.module.admin.service.AdminEpisodeService;
import com.project.module.admin.service.AdminLogService;
import com.project.module.content.entity.Content;
import com.project.module.content.entity.Episode;
import com.project.module.content.mapper.ContentMapper;
import com.project.module.content.mapper.EpisodeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminEpisodeServiceImpl implements AdminEpisodeService {

    private final EpisodeMapper episodeMapper;
    private final ContentMapper contentMapper;
    private final AdminLogService adminLogService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public Episode addEpisode(Episode episode, Long adminId) {
        episodeMapper.insert(episode);
        updateContentTimestamp(episode.getContentId(), adminId);
        adminLogService.log(adminId, "CREATE", "EPISODE",
                String.valueOf(episode.getId()), null, episode);
        log.info("单集添加成功: id={}, contentId={}, episodeNumber={}",
                episode.getId(), episode.getContentId(), episode.getEpisodeNumber());
        return episode;
    }

    @Override
    @Transactional
    public Episode updateEpisode(Long id, Episode update, Long adminId) {
        Episode existing = episodeMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.EPISODE_NOT_FOUND);
        }

        String beforeJson = toJson(existing);

        update.setId(id);
        episodeMapper.updateById(update);
        updateContentTimestamp(existing.getContentId(), adminId);

        Episode updated = episodeMapper.selectById(id);
        adminLogService.log(adminId, "UPDATE", "EPISODE", String.valueOf(id), beforeJson, updated);

        log.info("单集更新成功: id={}", id);
        return updated;
    }

    @Override
    @Transactional
    public void deleteEpisode(Long id, Long adminId) {
        Episode episode = episodeMapper.selectById(id);
        if (episode == null) {
            throw new BusinessException(ErrorCode.EPISODE_NOT_FOUND);
        }

        // 软删除
        episode.setSortOrder(-1);
        episodeMapper.updateById(episode);

        adminLogService.log(adminId, "DELETE", "EPISODE", String.valueOf(id), episode, null);
        log.info("单集删除成功: id={}", id);
    }

    @Override
    @Transactional
    public int batchAddEpisodes(BatchAddEpisodesRequest request, Long adminId) {
        // 1. 校验内容存在
        Content content = contentMapper.selectById(request.getContentId());
        if (content == null || content.getStatus() == -1) {
            throw new BusinessException(ErrorCode.CONTENT_NOT_FOUND);
        }

        // 2. 批量构建实体
        List<Episode> episodes = new ArrayList<>();
        for (BatchAddEpisodesRequest.EpisodeItem item : request.getEpisodes()) {
            Episode episode = new Episode();
            episode.setContentId(request.getContentId());
            episode.setEpisodeNumber(item.getEpisodeNumber());
            episode.setTitle(item.getTitle());
            episode.setVideoUrl(item.getVideoUrl());
            episode.setDuration(item.getDuration());
            episode.setIsFree(item.getIsFree() != null ? item.getIsFree() : 0);
            episode.setSortOrder(item.getSortOrder() != null ? item.getSortOrder() : item.getEpisodeNumber());
            episode.setSourceType(item.getSourceType() != null ? item.getSourceType() : 1);
            episodes.add(episode);
        }

        // 3. 批量插入
        int count = episodeMapper.batchInsert(episodes);

        // 4. 更新content时间戳
        updateContentTimestamp(request.getContentId(), adminId);

        // 5. 记录日志
        adminLogService.log(adminId, "BATCH_CREATE", "EPISODE",
                String.valueOf(request.getContentId()), null,
                Map.of("count", count, "episodes", request.getEpisodes()));

        log.info("批量添加剧集成功: contentId={}, count={}", request.getContentId(), count);
        return count;
    }

    @Override
    @Transactional
    public void sortEpisodes(SortEpisodesRequest request, Long adminId) {
        // 批量更新sort_order
        for (SortEpisodesRequest.EpisodeOrder order : request.getEpisodeOrders()) {
            Episode episode = new Episode();
            episode.setId(order.getEpisodeId());
            episode.setSortOrder(order.getSortOrder());
            episodeMapper.updateById(episode);
        }

        updateContentTimestamp(request.getContentId(), adminId);

        adminLogService.log(adminId, "UPDATE", "EPISODE",
                "sort:" + request.getContentId(), null,
                request.getEpisodeOrders().stream()
                        .collect(Collectors.toMap(
                                SortEpisodesRequest.EpisodeOrder::getEpisodeId,
                                SortEpisodesRequest.EpisodeOrder::getSortOrder)));

        log.info("剧集排序完成: contentId={}, count={}",
                request.getContentId(), request.getEpisodeOrders().size());
    }

    private void updateContentTimestamp(Long contentId, Long adminId) {
        Content content = contentMapper.selectById(contentId);
        if (content != null) {
            content.setUpdatedBy(adminId);
            contentMapper.updateById(content);
        }
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
