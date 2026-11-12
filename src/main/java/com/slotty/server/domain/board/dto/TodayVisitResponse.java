package com.slotty.server.domain.board.dto;

import com.slotty.server.domain.visit.VisitState;

import java.time.OffsetDateTime;

// 오늘 방문
public record TodayVisitResponse(
        Long visitId,
        String plateNumber,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        Long slotId,
        VisitState state
) {
}
