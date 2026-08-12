package com.project.module.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "签发管理员账号请求")
public class AdminAccountCreateRequest {
    @NotBlank
    @Size(min = 3, max = 50)
    @Schema(description = "登录用户名")
    private String username;

    @NotBlank
    @Size(min = 8, max = 72)
    @Schema(description = "初始密码，至少8位")
    private String password;

    @NotBlank
    @Schema(description = "角色：SUPER_ADMIN、ADMIN、OPERATOR")
    private String role;
}
