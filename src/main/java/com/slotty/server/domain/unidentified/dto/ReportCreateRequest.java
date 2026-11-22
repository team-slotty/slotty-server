package com.slotty.server.domain.unidentified.dto;

import com.slotty.server.domain.vehicle.dto.VehiclePlate;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

// 43. 미확인 차량 기록
public record ReportCreateRequest(
        @NotNull Long slotId,
        @Pattern(regexp = VehiclePlate.PATTERN, message = VehiclePlate.MESSAGE) String plateNumber // 모르면 생략 (null은 @Pattern 통과)
) {
}
