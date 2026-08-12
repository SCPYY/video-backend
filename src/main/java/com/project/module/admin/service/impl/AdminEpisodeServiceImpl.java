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
import com.project.module.product.entity.Product;
import com.project.module.product.mapper.ProductMapper;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.springframework.beans.factory.annotation.Value;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminEpisodeServiceImpl implements AdminEpisodeService {

    @Value("${app.upload.root:uploads}")
    private String uploadRoot;
    @Value("${app.media.ffprobe-path:ffprobe}")
    private String ffprobePath;

    private final EpisodeMapper episodeMapper;
    private final ContentMapper contentMapper;
    private final AdminLogService adminLogService;
    private final ProductMapper productMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<Episode> listEpisodes(Long contentId) {
        return episodeMapper.selectList(new LambdaQueryWrapper<Episode>()
                .eq(Episode::getContentId, contentId)
                .ne(Episode::getSortOrder, -1)
                .orderByAsc(Episode::getSortOrder).orderByAsc(Episode::getEpisodeNumber));
    }

    @Override
    public Episode getEpisode(Long id) {
        Episode episode = episodeMapper.selectById(id);
        if (episode == null || Integer.valueOf(-1).equals(episode.getSortOrder())) {
            throw new BusinessException(ErrorCode.EPISODE_NOT_FOUND);
        }
        return episode;
    }

    @Override
    @Transactional
    public Episode addEpisode(Episode episode, Long adminId) {
        fillDuration(episode);
        normalizeAccess(episode);
        episodeMapper.insert(episode);
        syncEpisodeProduct(episode);
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

        normalizeAccess(update);
        fillDuration(update);
        update.setId(id);
        episodeMapper.updateById(update);
        syncEpisodeProduct(episodeMapper.selectById(id));
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
        productMapper.delete(new LambdaQueryWrapper<Product>().eq(Product::getEpisodeId, id).eq(Product::getType, 1));

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
            episode.setAccessType(item.getAccessType() != null ? item.getAccessType() : (episode.getIsFree() == 1 ? 1 : 2));
            episode.setPricePlatformCoin(item.getPricePlatformCoin());
            episode.setIsPreview(0);
            episode.setStatus(0);
            episode.setViewCount(0L);
            episode.setPlayCount(0L);
            episode.setSortOrder(item.getSortOrder() != null ? item.getSortOrder() : item.getEpisodeNumber());
            episode.setSourceType(item.getSourceType() != null ? item.getSourceType() : 1);
            fillDuration(episode);
            episodes.add(episode);
        }

        // 3. 批量插入
        int count = episodeMapper.batchInsert(episodes);
        // 批量插入不依赖 JDBC 回填主键，重新查询后再创建对应商品。
        episodes.forEach(item -> {
            Episode saved = episodeMapper.selectOne(new LambdaQueryWrapper<Episode>()
                    .eq(Episode::getContentId, item.getContentId())
                    .eq(Episode::getEpisodeNumber, item.getEpisodeNumber()));
            if (saved != null) syncEpisodeProduct(saved);
        });

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

    private void normalizeAccess(Episode episode) {
        int access = episode.getAccessType() == null ? (Integer.valueOf(1).equals(episode.getIsFree()) ? 1 : 2) : episode.getAccessType();
        if (access < 1 || access > 3) throw new BusinessException(ErrorCode.PARAM_ERROR, "分集访问类型无效");
        episode.setAccessType(access);
        episode.setIsFree(access == 1 ? 1 : 0);
        if (access == 2 && (episode.getPricePlatformCoin() == null || episode.getPricePlatformCoin().signum() <= 0)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "付费集必须填写平台币价格");
        }
        if (access != 2) episode.setPricePlatformCoin(null);
    }

    private void fillDuration(Episode episode) {
        if (episode.getDuration() != null && episode.getDuration() > 0) return;
        String url = episode.getVideoUrl();
        if (url == null || url.isBlank() || !url.contains("/uploads/videos/")) return;
        String filename = url.substring(url.lastIndexOf('/') + 1).split("\\?")[0];
        Path file = Paths.get(uploadRoot).toAbsolutePath().normalize().resolve("videos").resolve(filename).normalize();
        if (!file.startsWith(Paths.get(uploadRoot).toAbsolutePath().normalize()) || !java.nio.file.Files.exists(file)) return;
        try {
            Process process = new ProcessBuilder(ffprobePath, "-v", "error", "-show_entries", "format=duration", "-of", "default=noprint_wrappers=1:nokey=1", file.toString()).redirectErrorStream(true).start();
            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) { output = reader.lines().findFirst().orElse("").trim(); }
            if (process.waitFor() == 0 && !output.isBlank()) episode.setDuration(Math.max(1, (int) Math.ceil(Double.parseDouble(output))));
        } catch (Exception e) {
            log.warn("分集视频时长识别失败: videoUrl={}, ffprobePath={}", url, ffprobePath);
        }
    }

    private void syncEpisodeProduct(Episode episode) {
        LambdaQueryWrapper<Product> query = new LambdaQueryWrapper<Product>().eq(Product::getEpisodeId, episode.getId()).eq(Product::getType, 1);
        Product product = productMapper.selectOne(query);
        if (episode.getAccessType() == 2) {
            if (product == null) { product = new Product(); product.setType(1); product.setContentId(episode.getContentId()); product.setEpisodeId(episode.getId()); product.setName(episode.getTitle() == null ? "单集解锁" : episode.getTitle() + "解锁"); product.setStatus(1); }
            product.setPricePlatformCoin(episode.getPricePlatformCoin());
            product.setName(episode.getTitle() == null ? "单集解锁" : episode.getTitle() + "解锁");
            if (product.getId() == null) productMapper.insert(product); else productMapper.updateById(product);
        } else if (product != null) {
            productMapper.deleteById(product.getId());
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
