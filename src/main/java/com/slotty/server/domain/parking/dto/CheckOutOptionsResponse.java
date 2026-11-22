package com.slotty.server.domain.parking.dto;

import com.slotty.server.domain.parking.CheckOutMode;
import com.slotty.server.domain.parking.ParkingRecord;

import java.time.LocalDateTime;
import java.util.List;

public record CheckOutOptionsResponse(
        CheckOutMode mode,
        List<ParkedVehicle> vehicles,
        Long suggestedVehicleId // AUTO일 때만
) {
    public record ParkedVehicle(
            Long vehicleId,
            String plateNumber,
            Long slotId,
            String slotLabel,
            LocalDateTime enteredAt
    ) {
        // 열린 기록 하나 → 주차 중인 차 하나
        public static ParkedVehicle from(ParkingRecord record) {
            return new ParkedVehicle(
                    record.getVehicle().getId(),
                    record.getVehicle().getPlateNumber(),
                    record.getSlot().getId(),
                    record.getSlot().getLabel(),
                    record.getEnteredAt()
            );
        }
    }
}
