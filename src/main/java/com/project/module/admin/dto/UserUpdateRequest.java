package com.project.module.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "管理端用户基本资料修改请求")
public class UserUpdateRequest {
    @Size(max = 64) private String nickname;
    @Email @Size(max = 128) private String email;
    @Size(max = 32) private String phone;
    @Size(max = 255) private String avatarUrl;
}
