package com.slotty.server.domain.board.dto;

import com.slotty.server.domain.visit.Visit;

import java.time.LocalDateTime;

// 면에 지정된 방문 (관리자·면 주인·방문 세대에게만)
public record BoardVisitResponse(
        Long visitId,
        String unitNumber,  // 방문 세대
        String plateLast4,
        LocalDateTime startAt,
        LocalDateTime endAt
) {
    public static BoardVisitResponse from(Visit visit) {
        return new BoardVisitResponse(visit.getId(), visit.getHousehold().getUnitNumber(),
                visit.getPlateLast4(), visit.getStartAt(), visit.getEndAt());
    }
}
