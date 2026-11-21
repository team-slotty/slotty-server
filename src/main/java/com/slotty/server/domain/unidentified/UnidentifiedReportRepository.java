package com.slotty.server.domain.unidentified;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UnidentifiedReportRepository extends JpaRepository<UnidentifiedReport, Long> {

    // 이 면의 미해제 미확인 기록 (open_slot_id 유니크라 최대 1건)
    Optional<UnidentifiedReport> findBySlotIdAndResolvedAtIsNull(Long slotId);
}
