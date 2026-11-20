package com.slotty.server.domain.board.dto;

import com.slotty.server.domain.board.VehicleState;

// 내 차 카드
public record MyVehicleResponse(
        Long vehicleId,
        String plateNumber,
        VehicleState state,
        Integer slotNumber // 주차 중이 아닐 때 null이 들어가야 하므로 int 아닌 Integer
) {
}
