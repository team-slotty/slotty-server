package com.slotty.server.global.scheduler;

import com.slotty.server.domain.parking.ParkingRecordRepository;
import com.slotty.server.domain.unidentified.UnidentifiedReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// 보관 기간이 지난 기록 삭제 (스케줄러와 분리해 두어 직접 호출로 테스트할 수 있음)
@Service
@RequiredArgsConstructor
public class RecordCleanupService {

    public static final int PARKING_RECORD_KEEP_DAYS = 30;    // 입출차 기록: 출차 후 30일 (명세 34)
    public static final int UNIDENTIFIED_KEEP_DAYS = 7;       // 미확인 기록: 해제 후 7일 (명세 46)

    private final ParkingRecordRepository parkingRecordRepository;
    private final UnidentifiedReportRepository unidentifiedReportRepository;

    // 출차한 지 30일 지난 기록. 주차 중(exited_at NULL)인 기록은 오래돼도 지우지 않음
    @Transactional
    public int deleteOldParkingRecords(LocalDateTime now) {
        return parkingRecordRepository.deleteExitedBefore(now.minusDays(PARKING_RECORD_KEEP_DAYS));
    }

    // 해제한 지 7일 지난 미확인 기록. 미해제 기록은 지우지 않음
    @Transactional
    public int deleteOldUnidentifiedReports(LocalDateTime now) {
        return unidentifiedReportRepository.deleteResolvedBefore(now.minusDays(UNIDENTIFIED_KEEP_DAYS));
    }
}
