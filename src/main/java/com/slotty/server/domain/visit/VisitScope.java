package com.slotty.server.domain.visit;

// 37. 방문 목록 구간 (5-1 화면의 세 탭)
public enum VisitScope {
    UPCOMING, // SCHEDULED
    ONGOING,  // ONGOING + NEEDS_CHECK
    PAST      // ENDED
}
