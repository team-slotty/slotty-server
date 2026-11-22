package com.slotty.server.domain.unidentified.dto;

import com.slotty.server.domain.unidentified.ResolveType;
import com.slotty.server.domain.unidentified.UnidentifiedReport;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

// 46. 미확인 기록 이력 (명세 공통 페이지 형식)
public record ReportHistoryResponse(
        List<Item> items,
        int page,
        int size,
        long total
) {
    public record Item(
            Long reportId,
            String slotLabel,
            String plateNumber,      // 모르고 기록했으면 null
            UserRef reportedBy,
            LocalDateTime reportedAt,
            LocalDateTime resolvedAt, // 미해제면 null
            ResolveType resolveType   // LEFT | MINE | AUTO, 미해제면 null
    ) {
        static Item from(UnidentifiedReport report) {
            return new Item(
                    report.getId(),
                    report.getSlot().getLabel(),
                    report.getPlateNumber(),
                    new UserRef(report.getReportedBy().getId(), report.getReportedBy().getName()),
                    report.getReportedAt(),
                    report.getResolvedAt(),
                    report.getResolveType()
            );
        }
    }

    public record UserRef(Long userId, String name) {
    }

    public static ReportHistoryResponse from(Page<UnidentifiedReport> page) {
        return new ReportHistoryResponse(
                page.getContent().stream().map(Item::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements()
        );
    }
}
