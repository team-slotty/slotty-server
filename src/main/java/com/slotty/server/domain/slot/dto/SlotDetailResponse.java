package com.slotty.server.domain.slot.dto;

import com.slotty.server.domain.board.SlotStatus;
import com.slotty.server.domain.board.dto.AvailableWindowResponse;

import java.time.LocalDateTime;

public record SlotDetailResponse(
        Long slotId,
        String label,
        boolean isCommon,
        String assignedUnitNumber,   // 공용 면이면 null
        boolean isMine,              // 내 세대 배정 면
        SlotStatus status,
        Occupant occupant,           // 주차 중인 차 (없으면 null)
        AvailableWindowResponse availableWindow, // 지금 가용시간 회차면 그 시간 (없으면 null)
        Unidentified unidentified,   // 미해제 미확인 기록 (없으면 null)
        Permissions permissions
) {
    public record Occupant(
            String unitNumber,
            String plateLast4,
            String plateNumber,      // 내 세대 차량일 때만 (개인정보)
            LocalDateTime since
    ) {
    }

    public record Unidentified(
            Long reportId,
            String plateNumber,      // 관리자·면 주인에게만
            LocalDateTime reportedAt
    ) {
    }

    public record Permissions(
            boolean canReportUnidentified,  // 미확인 기록 남기기
            boolean canResolveUnidentified, // [차량 떠남]
            boolean canCorrectMine          // [내 차예요]
    ) {
    }
}
