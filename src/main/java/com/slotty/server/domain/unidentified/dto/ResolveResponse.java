package com.slotty.server.domain.unidentified.dto;

import com.slotty.server.domain.unidentified.ResolveType;
import com.slotty.server.domain.unidentified.UnidentifiedReport;

import java.time.LocalDateTime;

// 44. [차량 떠남] 응답
public record ResolveResponse(Long reportId, LocalDateTime resolvedAt, ResolveType resolveType) {

    public static ResolveResponse from(UnidentifiedReport report) {
        return new ResolveResponse(report.getId(), report.getResolvedAt(), report.getResolveType());
    }
}
