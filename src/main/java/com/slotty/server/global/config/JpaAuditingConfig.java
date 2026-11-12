package com.slotty.server.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// JPA Auditing (@CreatedDate 자동 기록) 활성화.
// 메인 클래스에 두면 @WebMvcTest에서 오류가 나서 분리함
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
