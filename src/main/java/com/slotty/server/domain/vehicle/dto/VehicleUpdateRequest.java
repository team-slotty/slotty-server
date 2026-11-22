package com.slotty.server.domain.vehicle.dto;

import jakarta.validation.constraints.Pattern;

// PATCH: 보낸 필드만 바꿈 (null = 그대로)
public record VehicleUpdateRequest(
        @Pattern(regexp = VehiclePlate.PATTERN, message = VehiclePlate.MESSAGE) // null이면 검사하지 않음
        String plateNumber,
        Long contactUserId
) {
}
