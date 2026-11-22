package com.slotty.server.domain.visit.dto;

import com.slotty.server.domain.user.User;
import com.slotty.server.domain.visit.Visit;
import com.slotty.server.domain.visit.VisitState;

import java.time.LocalDateTime;

// 38. 방문 상세 = 방문 객체 + 등록자 연락처 (면 주인이 "확인 필요" 방문 세대에 연락할 때)
public record VisitDetailResponse(
        Long visitId,
        String plateNumber,
        LocalDateTime startAt,
        LocalDateTime endAt,
        Long slotId,
        String slotLabel,
        LocalDateTime slotCancelledAt,
        VisitState state,
        LocalDateTime endedAt,
        VisitResponse.UserRef registeredBy,
        Contact contact
) {
    public static VisitDetailResponse of(Visit visit, LocalDateTime now) {
        VisitResponse base = VisitResponse.of(visit, now);
        User registrant = visit.getRegisteredBy();
        boolean canCall = registrant.isPhonePublic() && registrant.getDeletedAt() == null;
        return new VisitDetailResponse(
                base.visitId(), base.plateNumber(), base.startAt(), base.endAt(),
                base.slotId(), base.slotLabel(), base.slotCancelledAt(), base.state(),
                base.endedAt(), base.registeredBy(),
                new Contact(canCall, canCall ? registrant.getPhone() : null)
        );
    }

    public record Contact(boolean canCall, String phone) {
    }
}
