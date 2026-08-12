package com.project.module.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "热门标签")
public class HotTagVO {

    @Schema(description = "标签名称", example = "逆袭")
    private String tag;

    @Schema(description = "搜索次数", example = "128")
    private Long searchCount;
}
