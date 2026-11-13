package com.slotty.server.domain.board.dto;

import java.time.OffsetDateTime;

// 면에 지정된 방문
public record BoardVisitResponse(
        Long visitId,
        String unitNumber,
        String plateLast4,
        OffsetDateTime startAt,
        OffsetDateTime endAt
) {
}
