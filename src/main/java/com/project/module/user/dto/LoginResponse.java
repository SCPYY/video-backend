package com.project.module.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "登录/注册/刷新Token响应")
public class LoginResponse {

    @Schema(description = "访问令牌", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String accessToken;

    @Schema(description = "刷新令牌", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String refreshToken;

    @Schema(description = "访问令牌过期时间（秒）", example = "7200")
    private Long expiresIn;

    @Schema(description = "Token类型", example = "Bearer")
    private String tokenType;

    @Schema(description = "用户信息")
    private UserInfo user;

    @Data
    @Builder
    @Schema(description = "用户基础信息")
    public static class UserInfo {

        @Schema(description = "用户ID", example = "1")
        private Long id;

        @Schema(description = "用户名", example = "testuser")
        private String username;

        @Schema(description = "昵称", example = "测试用户")
        private String nickname;

        @Schema(description = "头像URL")
        private String avatarUrl;

        @Schema(description = "邮箱", example = "test@example.com")
        private String email;
    }
}
