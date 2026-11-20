package com.slotty.server.domain.visit;

// 방문 상태는 현황판뿐만 아니라 방문 목록 API에서도 사용하므로 domain/visit/ 에 둠
public enum VisitState {
    SCHEDULED, ONGOING, NEEDS_CHECK, ENDED
}
