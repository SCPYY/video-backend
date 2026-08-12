package com.project.module.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "内容列表查询参数")
public class ContentListQuery {

    @Schema(description = "类型：1-短剧 2-影游", example = "1")
    private Integer type;

    @Schema(description = "状态：0-下架 1-上架", example = "1")
    private Integer status;

    @Schema(description = "分类ID", example = "1")
    private Long category;

    @Schema(description = "关键词搜索（标题模糊匹配）", example = "霸道")
    private String keyword;

    @Schema(description = "页码", example = "1")
    private Integer page = 1;

    @Schema(description = "每页条数", example = "10")
    private Integer size = 10;
}
