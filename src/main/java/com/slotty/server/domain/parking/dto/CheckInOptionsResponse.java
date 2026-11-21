package com.slotty.server.domain.parking.dto;

import com.slotty.server.domain.board.VehicleState;
import com.slotty.server.domain.parking.CheckInMode;
import com.slotty.server.domain.parking.SlotType;

import java.util.List;

public record CheckInOptionsResponse(
        CheckInMode mode,
        List<VehicleOption> vehicles,
        Long suggestedVehicleId, // AUTO일 때만
        SlotOptions slots
) {
    public record VehicleOption(
            Long vehicleId,
            String plateNumber,
            VehicleState state,     // DRIVING | PARKED
            String parkedSlotLabel  // 주차 중이면 그 면 이름
    ) {
    }

    public record SlotOptions(
            Long suggestedSlotId,         // 내 배정 면 중 빈 면. 없으면 null
            boolean assignedAllOccupied,  // 배정 면이 있는데 모두 차 있음 → 공용 면 선택
            List<SlotOption> options      // 빈 배정 면 → 빈 공용 면 순서
    ) {
    }

    public record SlotOption(Long slotId, String label, SlotType type) {
    }
}
