package com.slotty.server.domain.user.dto;

import com.slotty.server.domain.villa.EntranceSide;
import com.slotty.server.domain.villa.LayoutTemplate;

import java.math.BigDecimal;
import java.util.List;

public record MeResponse(
        Long id,
        String loginId,
        String name,
        String phone,
        boolean phonePublic,
        boolean isAdmin,
        HouseholdInfo household, // 소속 전이면 null
        VillaInfo villa          // 소속 전이면 null
) {
    public record HouseholdInfo(
            Long id,
            String unitNumber,
            List<Long> assignedSlotIds
    ) {
    }

    public record VillaInfo(
            Long id,
            String name,
            LayoutTemplate layoutTemplate,
            EntranceSide entranceSide,
            Anchor anchor
    ) { // 좌표가 없으면 anchor null → 도착·출발 알림 비활성
    }

    public record Anchor(
            BigDecimal latitude,
            BigDecimal longitude
    ) {
    }
}
