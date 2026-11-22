package com.slotty.server.domain.board.dto;

import com.slotty.server.domain.board.VehicleState;

// 현황판의 내 차 (명세 22: slotId. 기존 /api/vehicles/me의 slotNumber와 값은 같고 이름만 명세대로)
public record BoardVehicleResponse(
        Long vehicleId,
        String plateNumber,
        VehicleState state, // PARKED | DRIVING
        Long slotId         // 주차 중이 아니면 null
) {
}
