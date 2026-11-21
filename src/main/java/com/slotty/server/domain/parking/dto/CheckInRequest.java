package com.slotty.server.domain.parking.dto;

import com.slotty.server.domain.parking.EnterMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record CheckInRequest(
        @NotNull Long vehicleId,
        @NotNull Long slotId,
        @NotNull EnterMethod method,                   // NOTIFICATION | MANUAL
        @NotBlank @Size(max = 36) String clientRequestId, // 앱이 만든 UUID (재전송 중복 방지)
        OffsetDateTime enteredAt                       // 탭한 시각 (+09:00 포함). 생략하면 서버 시각
) {
}
