package com.slotty.server.domain.visit.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.slotty.server.domain.slot.ParkingSlot;
import com.slotty.server.domain.visit.VisitSlotPolicy;

import java.time.LocalTime;

// 36. 방문 지정 가능한 면
public record AvailableSlotResponse(
        Long slotId,
        String label,
        String type,               // COMMON(공용) | AVAILABLE(가용시간이 열린 배정 면)
        String assignedUnitNumber, // 공용이면 null
        boolean allDay,            // 공용이면 false
        TimeWindow window          // 종일·공용이면 null
) {
    public static AvailableSlotResponse of(ParkingSlot slot, VisitSlotPolicy.Window window) {
        if (window.common()) {
            return new AvailableSlotResponse(slot.getId(), slot.getLabel(), "COMMON", null, false, null);
        }
        return new AvailableSlotResponse(
                slot.getId(),
                slot.getLabel(),
                "AVAILABLE",
                slot.getHousehold().getUnitNumber(),
                window.allDay(),
                window.allDay() ? null : new TimeWindow(window.startTime(), window.endTime())
        );
    }

    public record TimeWindow(
            @JsonFormat(pattern = "HH:mm") LocalTime startTime,
            @JsonFormat(pattern = "HH:mm") LocalTime endTime
    ) {
    }
}
