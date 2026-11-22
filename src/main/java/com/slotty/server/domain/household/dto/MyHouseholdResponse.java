package com.slotty.server.domain.household.dto;

import java.util.List;

public record MyHouseholdResponse(
        Long householdId,
        String unitNumber,
        List<AssignedSlot> assignedSlots,
        List<Member> members // 같은 세대라도 전화번호는 내려주지 않음 (명세)
) {
    public record AssignedSlot(Long slotId, String label) {
    }

    public record Member(Long userId, String name) {
    }
}
