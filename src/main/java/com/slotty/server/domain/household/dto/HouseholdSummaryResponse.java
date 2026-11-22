package com.slotty.server.domain.household.dto;

import java.util.List;

// 관리자 세대 목록의 한 줄 (7-3 세대·면 관리, 7-5 초대 코드, 7-6 세대원 관리)
public record HouseholdSummaryResponse(
        Long householdId,
        String unitNumber,
        String inviteCode,
        List<String> slotLabels,
        long vehicleCount,
        List<Member> members
) {
    public record Member(Long userId, String name, boolean isAdmin) {
    }
}
