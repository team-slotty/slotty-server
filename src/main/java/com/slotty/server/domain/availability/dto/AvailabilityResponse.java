package com.slotty.server.domain.availability.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.slotty.server.domain.availability.Availability;
import com.slotty.server.domain.availability.Weekday;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record AvailabilityResponse(
        Long availabilityId,
        Long slotId,
        String slotLabel,
        boolean isRecurring,
        List<Weekday> days,          // 반복이 아니면 null
        LocalDate targetDate,        // 반복이면 null
        boolean allDay,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm") LocalTime endTime,
        List<LocalDate> skippedDates // 오늘 이후의 [이번만 해제] 날짜
) {
    public static AvailabilityResponse of(Availability availability, List<LocalDate> skippedDates) {
        return new AvailabilityResponse(
                availability.getId(),
                availability.getSlot().getId(),
                availability.getSlot().getLabel(),
                availability.isRecurring(),
                availability.isRecurring() ? Weekday.fromMask(availability.getDaysMask()) : null,
                availability.getTargetDate(),
                availability.isAllDay(),
                availability.getStartTime(),
                availability.getEndTime(),
                skippedDates
        );
    }
}
