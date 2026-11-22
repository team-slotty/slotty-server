package com.slotty.server.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Swagger(OpenAPI) 문서 설정: 제목·설명 + 우측 상단 [Authorize] 버튼(JWT Bearer)
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth"; // 아래 두 곳을 이어 주는 이름

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Slotty API")
                        .description("""
                                빌라 주차 관리 앱 Slotty 백엔드 API
                                1. POST /api/auth/login (또는 signup)으로 accessToken 발급
                                2. 우측 상단 [Authorize]에 accessToken만 붙여 넣기 (Bearer는 자동)
                                3. 테스트 계정: kim101 / slotty1234! (101호, 관리자)""")
                        .version("v1"))
                // 1) 인증 방식 정의: HTTP Authorization 헤더에 "Bearer {JWT}"
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                // 2) 모든 API에 적용 → Authorize 후 요청마다 헤더가 자동으로 붙음
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
