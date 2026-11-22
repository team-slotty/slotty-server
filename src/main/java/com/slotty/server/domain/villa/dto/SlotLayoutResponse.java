package com.slotty.server.domain.villa.dto;

import com.slotty.server.domain.slot.ParkingSlot;

import java.math.BigDecimal;

// 면 배치 한 칸 (온보딩·배치 변경 응답)
public record SlotLayoutResponse(
        Long slotId, String label,
        BigDecimal x, BigDecimal y, BigDecimal w, BigDecimal h, short rotation,
        String unitNumber   // 배정 세대 호수, 공용이면 null
) {
    public static SlotLayoutResponse from(ParkingSlot slot) {
        return new SlotLayoutResponse(
                slot.getId(), slot.getLabel(),
                slot.getX(), slot.getY(), slot.getW(), slot.getH(), slot.getRotation(),
                slot.getHousehold() == null ? null : slot.getHousehold().getUnitNumber());
    }
}
