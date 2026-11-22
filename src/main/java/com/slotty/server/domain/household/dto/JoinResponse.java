package com.slotty.server.domain.household.dto;

import com.slotty.server.domain.villa.Household;

public record JoinResponse(
        HouseholdInfo household,
        VillaInfo villa,
        boolean hasAssignedSlot // true면 앱이 가용시간 메뉴를 보여줌
) {
    public record HouseholdInfo(Long id, String unitNumber) {
    }

    public record VillaInfo(Long id, String name) {
    }

    public static JoinResponse of(Household household, boolean hasAssignedSlot) {
        return new JoinResponse(
                new HouseholdInfo(household.getId(), household.getUnitNumber()),
                new VillaInfo(household.getVilla().getId(), household.getVilla().getName()),
                hasAssignedSlot
        );
    }
}
