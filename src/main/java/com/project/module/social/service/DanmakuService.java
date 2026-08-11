package com.project.module.social.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.project.module.social.dto.DanmakuRequest;
import com.project.module.social.dto.DanmakuVO;
import com.project.module.social.entity.Danmaku;

import java.util.List;

public interface DanmakuService extends IService<Danmaku> {

    /** 发送弹幕 */
    DanmakuVO sendDanmaku(DanmakuRequest request, Long userId);

    /** 获取指定时间段的弹幕 */
    List<DanmakuVO> getDanmakuList(Long episodeId, Long userId, Integer startTime, Integer endTime);

    /** 弹幕点赞（切换） */
    boolean toggleLike(Long id, Long userId);

    /** 删除弹幕（本人或管理员） */
    void deleteDanmaku(Long id, Long userId, boolean isAdmin);
}
