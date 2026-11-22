package com.slotty.server.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// @Scheduled 작업 켜기 (SSE heartbeat, 기록 정리)
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
