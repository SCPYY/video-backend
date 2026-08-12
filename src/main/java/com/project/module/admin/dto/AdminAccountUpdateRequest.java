package com.project.module.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "后台管理员账号修改请求")
public class AdminAccountUpdateRequest {
    @NotBlank
    @Schema(description = "登录用户名")
    private String username;
    @Schema(description = "角色，仅超级管理员可以修改；SUPER_ADMIN/ADMIN/OPERATOR")
    private String role;
    @Schema(description = "账号状态：0-正常，1-禁用")
    private Integer status;
}
