package com.slotty.server.domain.unidentified.controller;

import com.slotty.server.domain.unidentified.ReportStatus;
import com.slotty.server.domain.unidentified.dto.*;
import com.slotty.server.domain.unidentified.service.UnidentifiedReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/unidentified-reports")
public class UnidentifiedReportController {

    private final UnidentifiedReportService unidentifiedReportService;

    // 43. 미확인 차량 기록
    @PostMapping
    public ResponseEntity<ReportResponse> report(@AuthenticationPrincipal Long userId,
                                                 @Valid @RequestBody ReportCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(unidentifiedReportService.report(userId, request));
    }

    // 44. [차량 떠남] 해제
    @PostMapping("/{reportId}/resolve")
    public ResolveResponse resolve(@AuthenticationPrincipal Long userId, @PathVariable Long reportId) {
        return unidentifiedReportService.resolve(userId, reportId);
    }

    // 45. [내 차예요] 정정
    @PostMapping("/{reportId}/mine")
    public MineResponse correctMine(@AuthenticationPrincipal Long userId, @PathVariable Long reportId,
                                    @Valid @RequestBody MineRequest request) {
        return unidentifiedReportService.correctMine(userId, reportId, request);
    }

    // 46. 미확인 기록 이력 (관리자)
    @GetMapping
    public ReportHistoryResponse getHistory(@AuthenticationPrincipal Long userId,
                                            @RequestParam(defaultValue = "ALL") ReportStatus status,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return unidentifiedReportService.getHistory(userId, status, page, size);
    }
}
