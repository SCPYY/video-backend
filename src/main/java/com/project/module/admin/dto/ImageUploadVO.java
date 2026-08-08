package com.project.module.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "图片上传结果")
public class ImageUploadVO {

    @Schema(description = "图片访问URL")
    private String url;

    @Schema(description = "缩略图URL")
    private String thumbnailUrl;

    @Schema(description = "图片宽度")
    private Integer width;

    @Schema(description = "图片高度")
    private Integer height;

    @Schema(description = "文件大小（字节）")
    private Long size;
}
