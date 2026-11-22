package com.slotty.server.domain.board;

import com.slotty.server.domain.visit.Visit;
import com.slotty.server.domain.visit.VisitState;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

// 면 상태 판정 규칙 (명세 22번 표). 현황판(22)과 면 상세(24)가 같은 규칙을 쓰도록 한 곳에
// 위에서부터 먼저 맞는 것: UNIDENTIFIED > OCCUPIED > NEEDS_CHECK > VISITING > VISIT_SCHEDULED > AVAILABLE > EMPTY
public final class SlotStatusResolver {

    private SlotStatusResolver() {
    }

    public static SlotStatus resolve(boolean hasOpenReport, boolean hasOpenRecord, Visit visit,
                                     LocalDateTime now, boolean availableNow) {
        if (hasOpenReport) {
            return SlotStatus.UNIDENTIFIED;
        }
        if (hasOpenRecord) {
            return SlotStatus.OCCUPIED;
        }
        if (visit != null) {
            // 방문 상태 규칙은 Visit.stateAt 하나만 씀 (종료 후 30분은 ONGOING → 방문 중)
            switch (visit.stateAt(now)) {
                case NEEDS_CHECK -> { return SlotStatus.NEEDS_CHECK; }
                case ONGOING -> { return SlotStatus.VISITING; }
                case SCHEDULED -> { return SlotStatus.VISIT_SCHEDULED; }
                default -> { }
            }
        }
        if (availableNow) {
            return SlotStatus.AVAILABLE;
        }
        return SlotStatus.EMPTY;
    }

    // 한 면에 지정된 방문이 여러 개일 때 화면에 보여줄 하나: 확인 필요 > 방문 중 > 오늘 가장 먼저 시작할 예정
    // 후보는 면 지정이 살아 있고 [방문 종료] 전인 방문 (쿼리에서 거름). 내일 이후 예정은 쿼리에서 이미 빠짐
    public static Visit pickVisit(List<Visit> candidates, LocalDateTime now) {
        return candidates.stream()
                .filter(v -> v.stateAt(now) != VisitState.ENDED)
                .min(Comparator.comparingInt((Visit v) -> priority(v.stateAt(now)))
                        .thenComparing(Visit::getStartAt))
                .orElse(null);
    }

    private static int priority(VisitState state) {
        return switch (state) {
            case NEEDS_CHECK -> 0;
            case ONGOING -> 1;
            case SCHEDULED -> 2;
            case ENDED -> 3;
        };
    }
}
