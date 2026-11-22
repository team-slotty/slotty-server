package com.slotty.server.domain.unidentified.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

// 45. [내 차예요] 정정
public record MineRequest(
        @NotNull Long vehicleId,
        @NotBlank @Size(max = 36) String clientRequestId, // 앱이 만든 UUID (입차와 같은 재전송 중복 방지)
        OffsetDateTime enteredAt                          // 생략하면 서버 시각
) {
}
