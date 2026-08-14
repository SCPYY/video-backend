package com.project.module.content.service;

import com.project.module.content.dto.*;
import com.project.module.content.entity.InteractiveProgress;
import com.project.module.content.entity.InteractiveScene;
import java.util.List;

public interface InteractiveService {
    InteractiveTreeVO tree(Long contentId);
    InteractiveScenePlayVO playScene(Long sceneId, Long userId, String baseUrl);
    InteractiveChooseVO choose(Long nodeId, InteractiveChooseRequest request, Long userId);
    InteractiveProgress progress(Long contentId, Long userId);
    void saveProgress(Long contentId, Long userId, Long sceneId, Long nodeId, Integer seconds);
    void reset(Long contentId, Long userId);
    List<InteractiveScene> endings(Long contentId, Long userId);
}
