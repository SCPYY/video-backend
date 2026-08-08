package com.project.module.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "登录/刷新Token响应")
public class LoginResponse {

    @Schema(description = "访问令牌", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String accessToken;

    @Schema(description = "刷新令牌", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String refreshToken;

    @Schema(description = "访问令牌过期时间（秒）", example = "7200")
    private Long expiresIn;

    @Schema(description = "Token类型", example = "Bearer")
    private String tokenType;
}
