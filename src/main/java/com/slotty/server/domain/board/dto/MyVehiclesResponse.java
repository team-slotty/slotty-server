package com.slotty.server.domain.board.dto;

import java.util.List;

public record MyVehiclesResponse(
        String unitNumber,
        List<MyVehicleResponse> vehicles
) {
}
