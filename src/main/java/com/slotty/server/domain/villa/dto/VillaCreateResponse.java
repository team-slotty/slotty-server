package com.slotty.server.domain.villa.dto;

import java.util.List;

public record VillaCreateResponse(
        VillaSummary villa,
        boolean geocodeFailed,                 // true면 anchor null → 관리 홈에 기준점 배너
        List<SlotLayoutResponse> slots,
        List<HouseholdCode> households         // 1-6 초대 코드 공유 화면
) {
    public record VillaSummary(Long id, String name, AnchorResponse anchor) {
    }

    public record HouseholdCode(Long householdId, String unitNumber, String inviteCode) {
    }
}
