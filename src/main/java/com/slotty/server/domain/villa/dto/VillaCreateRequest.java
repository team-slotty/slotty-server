package com.slotty.server.domain.villa.dto;

import com.slotty.server.domain.slot.SlotLayoutCalculator;
import com.slotty.server.domain.villa.EntranceSide;
import com.slotty.server.domain.villa.LayoutTemplate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

// 빌라 온보딩 (앱이 1-1 ~ 1-4 입력을 모아 한 번에 보냄)
public record VillaCreateRequest(
        @NotBlank @Size(max = 50) String name,
        @NotBlank @Size(max = 200) String address,
        @NotNull LayoutTemplate layoutTemplate,          // LINE | DOUBLE | L_SHAPE
        @NotNull EntranceSide entranceSide,              // TOP | BOTTOM | LEFT | RIGHT
        @NotNull @Min(SlotLayoutCalculator.MIN_SLOTS) @Max(SlotLayoutCalculator.MAX_SLOTS) Integer slotCount,
        @NotEmpty @Valid List<HouseholdInput> households,
        @NotBlank String myUnitNumber,                   // households 중 내 호수
        @Valid List<AssignmentInput> assignments         // 없는 면은 공용 (생략 가능)
) {
    public record HouseholdInput(@NotBlank @Size(max = 10) String unitNumber) {
    }

    public record AssignmentInput(@NotBlank String slotLabel, @NotBlank String unitNumber) {
    }
}
