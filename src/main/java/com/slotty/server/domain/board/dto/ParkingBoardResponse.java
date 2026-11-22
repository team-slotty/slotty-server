package com.slotty.server.domain.board.dto;

import java.time.LocalDateTime;
import java.util.List;

// 22. 주차 현황판 전체 응답 (시각은 다른 API와 같은 KST LocalDateTime)
public record ParkingBoardResponse(
        LocalDateTime serverTime,
        boolean anchorMissing,             // 기준점 없음 → 관리자 홈 배너
        List<BoardSlotResponse> slots,
        List<BoardVehicleResponse> myVehicles,
        List<TodayVisitResponse> todayVisits
) {
}
