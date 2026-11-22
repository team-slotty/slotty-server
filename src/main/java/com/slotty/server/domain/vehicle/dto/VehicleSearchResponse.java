package com.slotty.server.domain.vehicle.dto;

import java.util.List;

public record VehicleSearchResponse(
        int remainingToday,   // 오늘 남은 검색 횟수 (하루 20회)
        List<Result> results  // 최대 10건
) {
    public record Result(
            String type,          // RESIDENT (방문차량 VISIT은 방문 기능 추가 시)
            String unitNumber,
            String plateLast4,
            String plateNumber,   // 내 세대 차량일 때만 전체 번호
            Object visit,         // 방문차량일 때만 (지금은 항상 null)
            Contact contact
    ) {
    }

    public record Contact(
            boolean canCall,      // 연락 담당자가 연락처 공개에 동의했을 때만 true
            String phone
    ) {
    }
}
