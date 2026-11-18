package com.slotty.server.domain.board.dto;

import java.time.OffsetDateTime;

// 주차 중인 차
public record OccupantResponse(
        String unitNumber,
        String plateLast4,
        String plateNumber,
        OffsetDateTime since
) {
}
