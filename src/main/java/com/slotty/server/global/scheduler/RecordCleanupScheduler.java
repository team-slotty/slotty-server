package com.slotty.server.global.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

// 매일 새벽 4시(KST) 기록 정리. 시각은 설정으로 바꿀 수 있음 (확인할 때 10초마다 등)
@Slf4j
@Component
@RequiredArgsConstructor
public class RecordCleanupScheduler {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final RecordCleanupService recordCleanupService;

    @Scheduled(cron = "${cleanup.cron:0 0 4 * * *}", zone = "Asia/Seoul")
    public void cleanup() {
        LocalDateTime now = LocalDateTime.now(KST);
        // 두 작업을 따로 트랜잭션으로: 하나가 실패해도 다른 하나는 진행
        try {
            int records = recordCleanupService.deleteOldParkingRecords(now);
            log.info("입출차 기록 정리: {}건 삭제", records);
        } catch (Exception e) {
            log.error("입출차 기록 정리 실패", e);
        }
        try {
            int reports = recordCleanupService.deleteOldUnidentifiedReports(now);
            log.info("미확인 기록 정리: {}건 삭제", reports);
        } catch (Exception e) {
            log.error("미확인 기록 정리 실패", e);
        }
    }
}
