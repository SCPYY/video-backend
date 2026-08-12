package com.project.module.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "创建/更新内容请求")
public class ContentCreateRequest {

    @NotNull(message = "类型不能为空")
    @Schema(description = "类型：1-短剧 2-影游", example = "1")
    private Integer type;


    @NotBlank(message = "标题不能为空")
    @Schema(description = "标题", example = "新短剧")
    private String title;

    @Schema(description = "简介")
    private String description;

    @Schema(description = "封面图URL")
    private String coverUrl;

    @Schema(description = "分类")
    private String category;

    @Schema(description = "分类ID，优先使用该字段")
    private Long categoryId;

    @Schema(description = "标签（逗号分隔）")
    private String tags;

    @Schema(description = "状态：0-下架 1-上架", example = "1")
    private Integer status;

    @Schema(description = "排序权重", example = "100")
    private Integer sortOrder;

    @Schema(description = "管理员备注")
    private String adminRemark;

    @Schema(description = "幂等键（防重复提交）")
    private String idempotentKey;

    @Schema(description = "扩展属性（key-value）")
    private Map<String, Object> extras;
}
