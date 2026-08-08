package com.project.module.content.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.project.module.content.dto.EpisodePlayVO;
import com.project.module.content.dto.EpisodeVO;
import com.project.module.content.entity.Episode;

import java.util.List;

public interface EpisodeService extends IService<Episode> {

    /** 获取内容下的剧集列表 */
    List<EpisodeVO> getEpisodesByContentId(Long contentId);

    /** 获取播放信息（含鉴权） */
    EpisodePlayVO getPlayInfo(Long episodeId, Long userId);
}
