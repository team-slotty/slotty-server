package com.slotty.server.domain.parking.dto;

import com.slotty.server.domain.parking.ParkingRecord;

import java.time.LocalDateTime;

public record CheckOutResponse(
        Long recordId,
        Long slotId,
        String slotLabel,
        LocalDateTime exitedAt
) {
    public static CheckOutResponse from(ParkingRecord record) {
        return new CheckOutResponse(
                record.getId(),
                record.getSlot().getId(),
                record.getSlot().getLabel(),
                record.getExitedAt()
        );
    }
}
