package com.project.module.social.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "编辑评论请求")
public class CommentEditRequest {

    @Schema(description = "新内容", example = "修改后的评论")
    @NotBlank(message = "内容不能为空")
    @Size(max = 500, message = "内容最多500字")
    private String content;
}
