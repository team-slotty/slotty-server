package com.slotty.server.domain.unidentified.dto;

import com.slotty.server.domain.unidentified.UnidentifiedReport;

import java.time.LocalDateTime;

// 43. 기록 응답
public record ReportResponse(Long reportId, Long slotId, String plateNumber, LocalDateTime reportedAt) {

    public static ReportResponse from(UnidentifiedReport report) {
        return new ReportResponse(report.getId(), report.getSlot().getId(), report.getPlateNumber(), report.getReportedAt());
    }
}
