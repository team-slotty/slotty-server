package com.slotty.server.domain.parking.dto;

import com.slotty.server.domain.parking.ExitMethod;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record CheckOutRequest(
        @NotNull Long vehicleId,
        @NotNull ExitMethod method, // NOTIFICATION | MANUAL (AUTO는 서버 전용 → 400)
        OffsetDateTime exitedAt     // 탭한 시각 (+09:00 포함). 생략하면 서버 시각
) {
}
