package com.project.module.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
@Schema(description="管理端动态菜单。前端可根据 parentId、routePath 和 children 构建路由及二级菜单。")
public class AdminMenuVO {
    @Schema(description="菜单唯一 ID") private String id;
    @Schema(description="父菜单 ID；一级菜单为 null") private String parentId;
    @Schema(description="菜单名称") private String name;
    @Schema(description="菜单标题") private String title;
    @Schema(description="菜单类型：GROUP-分组，PAGE-页面") private String menuType;
    @Schema(description="前端页面路由路径") private String routePath;
    @Schema(description="前端组件标识；外层布局使用 Layout") private String component;
    @Schema(description="菜单图标名称") private String icon;
    @Schema(description="权限标识；前端按钮权限可据此判断") private String permission;
    @Schema(description="排序值，数字越小越靠前") private Integer sortOrder;
    @Schema(description="是否显示：true显示，false隐藏但仍可通过路由访问") private Boolean visible;
    @Schema(description="二级菜单列表") private List<AdminMenuVO> children;
}
