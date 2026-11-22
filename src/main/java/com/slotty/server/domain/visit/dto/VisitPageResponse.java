package com.slotty.server.domain.visit.dto;

import com.slotty.server.domain.visit.Visit;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

// 명세 공통 페이지 형식: { items, page, size, total }
public record VisitPageResponse(
        List<VisitResponse> items,
        int page,
        int size,
        long total
) {
    public static VisitPageResponse of(Page<Visit> page, LocalDateTime now) {
        return new VisitPageResponse(
                page.getContent().stream().map(v -> VisitResponse.of(v, now)).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements()
        );
    }
}
