package com.slotty.server.domain.household.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HouseholdCreateRequest(
        @NotBlank(message = "호수를 입력해 주세요.")
        @Size(max = 10, message = "호수는 10자 이하입니다.") // household.unit_number VARCHAR(10)
        String unitNumber
) {
}
