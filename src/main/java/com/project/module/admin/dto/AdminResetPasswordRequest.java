package com.project.module.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "管理员重置用户密码请求")
public class AdminResetPasswordRequest {
    @NotBlank @Size(min = 8, max = 72)
    @Schema(description = "新密码，至少 8 位") private String newPassword;
    @Size(max = 255) @Schema(description = "重置原因") private String reason;
}
