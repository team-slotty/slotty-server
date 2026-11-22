package com.slotty.server.domain.villa.dto;

import com.slotty.server.domain.villa.EntranceSide;
import com.slotty.server.domain.villa.LayoutTemplate;

public record VillaResponse(
        Long id, String name, String address,
        LayoutTemplate layoutTemplate, EntranceSide entranceSide,
        long slotCount, long householdCount,
        AnchorResponse anchor   // null 가능
) {
}
