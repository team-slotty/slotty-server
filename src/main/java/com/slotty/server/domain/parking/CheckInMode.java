package com.slotty.server.domain.parking;

// 입차 옵션의 차량 지정 방식 (운행 중인 차 대수로 결정)
public enum CheckInMode {
    AUTO,       // 운행 중 1대 → 자동 지정
    SELECT,     // 운행 중 2대 이상 → 사용자가 선택
    CORRECTION  // 운행 중 0대 → 기록 오류, 전체 차량에서 선택
}
