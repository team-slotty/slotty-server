package com.slotty.server.domain.board.dto;

import java.time.LocalDateTime;

// 주차 중인 차
public record OccupantResponse(
        String unitNumber,
        String plateLast4,
        String plateNumber, // 내 세대 차량일 때만 (F8 개인정보), 그 외 null
        LocalDateTime since
) {
}
