package com.project.module.content.dto;

import com.project.module.content.entity.InteractiveNode;
import com.project.module.content.entity.InteractiveOption;
import com.project.module.content.entity.InteractiveScene;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class InteractiveTreeVO {
    private Long contentId;
    private Long startSceneId;
    private List<SceneItem> scenes = new ArrayList<>();
    @Data public static class SceneItem { private InteractiveScene scene; private List<NodeItem> nodes = new ArrayList<>(); }
    @Data public static class NodeItem { private InteractiveNode node; private List<InteractiveOption> options = new ArrayList<>(); }
}
