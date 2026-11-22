package com.slotty.server.domain.visit.dto;

import com.slotty.server.domain.vehicle.dto.VehiclePlate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.OffsetDateTime;

// 35. 방문차량 등록
public record VisitCreateRequest(
        @NotBlank @Pattern(regexp = VehiclePlate.PATTERN, message = VehiclePlate.MESSAGE) String plateNumber,
        @NotNull OffsetDateTime startAt,
        @NotNull OffsetDateTime endAt,
        Long slotId // 생략하면 면 없이 등록 (차량 확인용)
) {
}
