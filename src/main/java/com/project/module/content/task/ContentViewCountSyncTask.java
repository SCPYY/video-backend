package com.project.module.content.task;

import com.project.module.content.mapper.ContentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContentViewCountSyncTask {
    private final StringRedisTemplate redis;
    private final ContentMapper contentMapper;

    @Scheduled(fixedDelay = 60000)
    public void sync() {
        Map<Object,Object> pending = redis.opsForHash().entries("content:view:pending");
        pending.forEach((key, value) -> {
            try {
                long delta = Long.parseLong(String.valueOf(value));
                if (delta > 0) contentMapper.addViewCount(Long.parseLong(String.valueOf(key)), delta);
                redis.opsForHash().delete("content:view:pending", key);
            } catch (Exception e) { log.warn("同步内容浏览量失败: contentId={}", key, e); }
        });
        Map<Object,Object> playUsers = redis.opsForHash().entries("content:play-user:pending");
        playUsers.forEach((key, value) -> {
            try {
                long delta = Long.parseLong(String.valueOf(value));
                if (delta > 0) contentMapper.addPlayUserCount(Long.parseLong(String.valueOf(key)), delta);
                redis.opsForHash().delete("content:play-user:pending", key);
            } catch (Exception e) { log.warn("同步播放用户数失败: contentId={}", key, e); }
        });
    }
}
