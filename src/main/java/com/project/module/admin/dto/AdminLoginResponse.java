package com.project.module.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "管理员登录响应")
public class AdminLoginResponse {

    @Schema(description = "访问令牌")
    private String accessToken;

    @Schema(description = "管理员ID")
    private Long adminId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "角色：ADMIN/EDITOR/VIEWER")
    private String role;
}
