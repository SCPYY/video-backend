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
@Schema(description = "视频上传结果")
public class VideoUploadVO {

    @Schema(description = "视频原始URL")
    private String url;

    @Schema(description = "HLS播放地址")
    private String playUrl;

    @Schema(description = "视频时长（秒）")
    private Integer duration;

    @Schema(description = "文件大小（字节）")
    private Long size;

    @Schema(description = "转码状态：PROCESSING/COMPLETED/FAILED")
    private String transcodeStatus;
}
