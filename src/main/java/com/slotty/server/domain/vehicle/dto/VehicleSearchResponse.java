package com.slotty.server.domain.vehicle.dto;

import java.time.LocalDateTime;
import java.util.List;

public record VehicleSearchResponse(
        int remainingToday,   // 오늘 남은 검색 횟수 (하루 20회)
        List<Result> results  // 최대 10건 (입주민 차량 먼저, 남은 자리에 방문차량)
) {
    public record Result(
            String type,          // RESIDENT | VISIT
            String unitNumber,    // 차량 세대 또는 방문 세대
            String plateLast4,
            String plateNumber,   // 내 세대 차량·내 세대 방문일 때만 전체 번호
            VisitInfo visit,      // 방문차량이고 내가 관리자·면 주인·방문 세대일 때만, 그 외 null
            Contact contact
    ) {
    }

    public record VisitInfo(
            Long visitId,
            LocalDateTime startAt,
            LocalDateTime endAt,
            String slotLabel      // 면 없이 등록했거나 면 지정이 취소됐으면 null
    ) {
    }

    public record Contact(
            boolean canCall,      // 입주민 차량은 연락 담당자, 방문은 등록자가 연락처 공개에 동의했을 때만 true
            String phone
    ) {
    }
}
