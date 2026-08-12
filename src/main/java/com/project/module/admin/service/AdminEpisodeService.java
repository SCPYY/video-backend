package com.project.module.admin.service;

import com.project.module.admin.dto.BatchAddEpisodesRequest;
import com.project.module.admin.dto.SortEpisodesRequest;
import com.project.module.content.entity.Episode;
import java.util.List;

public interface AdminEpisodeService {

    /**
     * 添加单集
     */
    Episode addEpisode(Episode episode, Long adminId);

    /**
     * 更新单集
     */
    Episode updateEpisode(Long id, Episode episode, Long adminId);

    /**
     * 删除单集（软删除）
     */
    void deleteEpisode(Long id, Long adminId);

    /**
     * 批量添加剧集
     */
    int batchAddEpisodes(BatchAddEpisodesRequest request, Long adminId);

    /**
     * 调整排序
     */
    void sortEpisodes(SortEpisodesRequest request, Long adminId);
    List<Episode> listEpisodes(Long contentId);
    Episode getEpisode(Long id);
}
