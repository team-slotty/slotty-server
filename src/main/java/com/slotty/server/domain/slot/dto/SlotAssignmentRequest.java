package com.slotty.server.domain.slot.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SlotAssignmentRequest(
        @NotEmpty @Valid List<Assignment> assignments // @Valid: 리스트 안 Assignment의 @NotNull까지 검사
) {
    public record Assignment(
            @NotNull Long slotId,
            Long householdId      // null이면 공용
    ) {
    }
}
