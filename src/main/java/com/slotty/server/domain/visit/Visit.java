package com.slotty.server.domain.visit;

import com.slotty.server.domain.slot.ParkingSlot;
import com.slotty.server.domain.user.User;
import com.slotty.server.domain.villa.Household;
import com.slotty.server.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "visit")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Visit extends BaseTimeEntity {

    // 면 지정 방문이 종료 시각 뒤 이만큼 지나도 [방문 종료]가 없으면 "확인 필요"
    public static final long NEEDS_CHECK_AFTER_MINUTES = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "household_id")
    private Household household; // 방문 세대

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id")
    private ParkingSlot slot; // 지정 면 (없으면 차량 확인용 방문)

    private LocalDateTime slotCancelledAt; // 면 주인이 가용시간을 해제해 면 지정이 취소된 시각

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registered_by")
    private User registeredBy;

    @Column(nullable = false, length = 12)
    private String plateNumber;

    @Column(nullable = false, length = 4)
    private String plateLast4;

    @Column(nullable = false)
    private LocalDateTime startAt;

    @Column(nullable = false)
    private LocalDateTime endAt;

    private LocalDateTime endedAt; // [방문 종료]를 누른 시각

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ended_by")
    private User endedBy;

    // 35. 등록
    public static Visit register(Household household, User registeredBy, String plateNumber,
                                 LocalDateTime startAt, LocalDateTime endAt, ParkingSlot slot) {
        Visit visit = new Visit();
        visit.household = household;
        visit.registeredBy = registeredBy;
        visit.changePlateNumber(plateNumber);
        visit.changePeriod(startAt, endAt);
        visit.slot = slot;
        return visit;
    }

    // 번호를 바꾸면 뒤 4자리(검색용)도 같이
    public void changePlateNumber(String plateNumber) {
        this.plateNumber = plateNumber;
        this.plateLast4 = plateNumber.substring(plateNumber.length() - 4);
    }

    public void changePeriod(LocalDateTime startAt, LocalDateTime endAt) {
        this.startAt = startAt;
        this.endAt = endAt;
    }

    // 39. 면 변경·해제: 새로 지정했거나 해제했으니 이전 취소 표시는 지움 (ck_visit_cancel: 면 없이 취소 시각만 있으면 안 됨)
    public void changeSlot(ParkingSlot slot) {
        this.slot = slot;
        this.slotCancelledAt = null;
    }

    // 면 주인이 가용시간을 해제·삭제하거나 면 배정이 바뀌어 면 지정의 근거가 사라짐 (면 지정은 남기고 표시만)
    public void cancelSlot(LocalDateTime at) {
        if (slot != null && slotCancelledAt == null) {
            this.slotCancelledAt = at;
        }
    }

    // 41. [방문 종료]
    public void end(User user, LocalDateTime at) {
        this.endedAt = at;
        this.endedBy = user;
    }

    // 면 지정이 살아 있는가 (지정 안 함·취소됨이면 false)
    public boolean hasActiveSlot() {
        return slot != null && slotCancelledAt == null;
    }

    public boolean isStarted(LocalDateTime now) {
        return !now.isBefore(startAt);
    }

    // 상태는 저장하지 않고 조회 시 계산 (ERD 결정)
    public VisitState stateAt(LocalDateTime now) {
        if (endedAt != null) {
            return VisitState.ENDED;
        }
        if (now.isBefore(startAt)) {
            return VisitState.SCHEDULED;
        }
        if (now.isBefore(endAt)) {
            return VisitState.ONGOING;
        }
        if (!hasActiveSlot()) {
            return VisitState.ENDED; // 면 없이 온 방문은 종료 시각에 자동 종료
        }
        // 면 지정 방문: 종료 후 30분은 차가 빠지는 중으로 보고 ONGOING, 그 뒤에는 확인 필요
        return now.isBefore(endAt.plusMinutes(NEEDS_CHECK_AFTER_MINUTES)) ? VisitState.ONGOING : VisitState.NEEDS_CHECK;
    }
}
