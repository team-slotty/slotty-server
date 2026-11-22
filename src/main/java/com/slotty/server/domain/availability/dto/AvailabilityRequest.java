package com.slotty.server.domain.availability.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.slotty.server.domain.availability.Weekday;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

// 값 조합(반복↔날짜, 종일↔시간) 검사는 서비스에서 → 400 AVAILABILITY_INVALID
public record AvailabilityRequest(
        @NotNull Long slotId,
        @NotNull Boolean isRecurring,
        List<Weekday> days,                          // 반복일 때 1개 이상
        LocalDate targetDate,                        // 반복이 아닐 때 (오늘 이후)
        @NotNull Boolean allDay,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime, // 종일이 아니면 30분 단위
        @JsonFormat(pattern = "HH:mm") LocalTime endTime
) {
}
