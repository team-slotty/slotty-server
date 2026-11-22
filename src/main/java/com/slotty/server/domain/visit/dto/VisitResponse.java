package com.slotty.server.domain.visit.dto;

import com.slotty.server.domain.slot.ParkingSlot;
import com.slotty.server.domain.user.User;
import com.slotty.server.domain.visit.Visit;
import com.slotty.server.domain.visit.VisitState;

import java.time.LocalDateTime;

// 방문 응답 공통 객체 (35, 37, 39, 41)
public record VisitResponse(
        Long visitId,
        String plateNumber,
        LocalDateTime startAt,
        LocalDateTime endAt,
        Long slotId,                   // 면 없이 등록했으면 null
        String slotLabel,
        LocalDateTime slotCancelledAt, // 값이 있으면 면 주인이 가용시간을 해제해 면 지정이 취소됨 → 앱이 안내
        VisitState state,              // SCHEDULED | ONGOING | NEEDS_CHECK | ENDED
        LocalDateTime endedAt,
        UserRef registeredBy
) {
    public static VisitResponse of(Visit visit, LocalDateTime now) {
        ParkingSlot slot = visit.getSlot();
        return new VisitResponse(
                visit.getId(),
                visit.getPlateNumber(),
                visit.getStartAt(),
                visit.getEndAt(),
                slot == null ? null : slot.getId(),
                slot == null ? null : slot.getLabel(),
                visit.getSlotCancelledAt(),
                visit.stateAt(now),
                visit.getEndedAt(),
                UserRef.from(visit.getRegisteredBy())
        );
    }

    public record UserRef(Long userId, String name) {
        static UserRef from(User user) {
            return new UserRef(user.getId(), user.getName());
        }
    }
}
