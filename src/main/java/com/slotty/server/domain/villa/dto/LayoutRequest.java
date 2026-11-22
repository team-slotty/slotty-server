package com.slotty.server.domain.villa.dto;

import com.slotty.server.domain.slot.SlotLayoutCalculator;
import com.slotty.server.domain.villa.EntranceSide;
import com.slotty.server.domain.villa.LayoutTemplate;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record LayoutRequest(
        @NotNull LayoutTemplate layoutTemplate,
        @NotNull EntranceSide entranceSide,
        @NotNull @Min(SlotLayoutCalculator.MIN_SLOTS) @Max(SlotLayoutCalculator.MAX_SLOTS) Integer slotCount
) {
}
