package com.slotty.server.domain.vehicle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VehicleCreateRequest(
        @NotBlank(message = "차량번호를 입력해 주세요.")
        @Pattern(regexp = VehiclePlate.PATTERN, message = VehiclePlate.MESSAGE)
        String plateNumber,   // "12가 3456"처럼 공백이 있어도 됨 → 서버가 공백 제거 후 저장

        Long contactUserId    // 연락 담당자 (내 세대원). 생략하면 본인
) {
}
