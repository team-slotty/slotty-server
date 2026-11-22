package com.slotty.server.domain.board.dto;

import com.slotty.server.domain.visit.Visit;
import com.slotty.server.domain.visit.VisitState;

import java.time.LocalDateTime;

// 오늘 방문 (우리 세대)
public record TodayVisitResponse(
        Long visitId,
        String plateNumber,
        LocalDateTime startAt,
        LocalDateTime endAt,
        Long slotId,          // 면 없이 등록했거나 면 지정이 취소됐으면 null
        VisitState state
) {
    public static TodayVisitResponse of(Visit visit, LocalDateTime now) {
        return new TodayVisitResponse(visit.getId(), visit.getPlateNumber(), visit.getStartAt(), visit.getEndAt(),
                visit.hasActiveSlot() ? visit.getSlot().getId() : null, visit.stateAt(now));
    }
}
