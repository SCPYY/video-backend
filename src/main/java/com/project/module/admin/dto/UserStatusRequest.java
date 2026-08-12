package com.project.module.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "管理端用户状态修改请求")
public class UserStatusRequest {
    @NotNull @Schema(description = "状态：0-正常，1-禁用，2-注销") private Integer status;
    @Size(max = 255) @Schema(description = "状态变更原因") private String reason;
}
