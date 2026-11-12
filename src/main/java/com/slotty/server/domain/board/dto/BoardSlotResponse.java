package com.slotty.server.domain.board.dto;

import com.slotty.server.domain.board.SlotStatus;

import java.math.BigDecimal;

// 면 한 칸
public record BoardSlotResponse(
        Long slotId,
        String label,
        BigDecimal x,
        BigDecimal y,
        BigDecimal w,
        BigDecimal h,
        short rotation,
        boolean isCommon,
        String assignedUnitNumber, // 공용이면 null
        boolean isMine,
        SlotStatus status,
        OccupantResponse occupant, // 빈 면이면 null
        AvailableWindowResponse availableWindow, // 가용시간 아니면 null
        BoardVisitResponse visit, // 권한 없거나 방문 없으면 null
        Long unidentifiedReportId // 미확인 아니면 null
) {
}
