package com.slotty.server.domain.parking;

// 출차 옵션의 차량 지정 방식 (주차 중인 차 대수로 결정)
public enum CheckOutMode {
    AUTO,   // 주차 중 1대 → 자동 지정
    SELECT  // 주차 중 2대 이상 → 사용자가 선택
}
