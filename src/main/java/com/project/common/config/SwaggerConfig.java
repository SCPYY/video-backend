package com.project.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("海外短剧+影游独立站 API")
                        .version("1.0.0")
                        .description("""
                                ## 模块说明
                                - **认证接口** — 注册/登录/Token刷新/登出
                                - **用户接口** — 个人资料管理
                                - **内容接口** — 短剧/影游浏览、搜索、详情
                                - **剧集接口** — 剧集列表、播放鉴权
                                - **商品接口** — 商品/套餐查询
                                - **订单接口** — 创建/查询/取消订单
                                - **权益接口** — 权益查询和鉴权

                                ## 认证说明
                                登录后获取 accessToken，在需要认证的接口中携带:
                                `Authorization: Bearer <accessToken>`
                                """)
                        .contact(new Contact().name("开发团队"))
                        .license(new License().name("内部使用")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("开发环境")
                ))
                .addSecurityItem(new SecurityRequirement().addList("Bearer"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("Bearer", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("输入登录获取的 accessToken")));
    }
}
