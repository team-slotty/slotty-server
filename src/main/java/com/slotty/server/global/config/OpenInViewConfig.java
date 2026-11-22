package com.slotty.server.global.config;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// OSIV(Open Session In View)를 SSE 경로만 빼고 켬
// OSIV는 요청이 끝날 때까지 EntityManager와 DB 커넥션을 붙잡아 둠. SSE 요청은 30분 동안 끝나지 않으므로
// 연결 10개면 커넥션 풀(기본 10개)이 바닥나 다른 API가 모두 멈춤 → application.yaml에서 기본 OSIV를 끄고
// 같은 인터셉터를 직접 등록하면서 SSE 경로만 제외 (나머지 API는 지금까지와 똑같이 동작)
@Configuration
@RequiredArgsConstructor
public class OpenInViewConfig implements WebMvcConfigurer {

    private final EntityManagerFactory entityManagerFactory;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        OpenEntityManagerInViewInterceptor interceptor = new OpenEntityManagerInViewInterceptor();
        interceptor.setEntityManagerFactory(entityManagerFactory);
        registry.addWebRequestInterceptor(interceptor)
                .excludePathPatterns("/api/parking-board/stream");
    }
}
