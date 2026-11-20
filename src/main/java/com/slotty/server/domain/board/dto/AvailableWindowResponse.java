package com.slotty.server.domain.board.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalTime;

// 가용시간
public record AvailableWindowResponse(
        boolean allDay,
        // "HH:mm:ss" 형태 -> "HH:mm" 형태
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm") LocalTime endTime
) {
}
