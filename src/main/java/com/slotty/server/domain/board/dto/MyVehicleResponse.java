package com.slotty.server.domain.board.dto;

import com.slotty.server.domain.board.VehicleState;

// 내 차 카드
public record MyVehicleResponse(
        Long vehicleId,
        String plateNumber,
        VehicleState state,
        Long slotId
) {
}
