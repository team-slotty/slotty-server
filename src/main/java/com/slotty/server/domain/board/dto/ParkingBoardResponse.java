package com.slotty.server.domain.board.dto;

import java.time.OffsetDateTime;
import java.util.List;

// 전체 응답
public record ParkingBoardResponse(
        OffsetDateTime serverTime,
        boolean anchorMissing,
        List<BoardSlotResponse> slots,
        List<MyVehicleResponse> myVehicles,
        List<TodayVisitResponse> todayVisits
) {
}
