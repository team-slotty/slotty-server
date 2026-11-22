package com.slotty.server.domain.visit.controller;

import com.slotty.server.domain.visit.VisitScope;
import com.slotty.server.domain.visit.dto.*;
import com.slotty.server.domain.visit.service.VisitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/visits")
public class VisitController {

    private final VisitService visitService;

    // 35. 방문차량 등록
    @PostMapping
    public ResponseEntity<VisitResponse> register(@AuthenticationPrincipal Long userId,
                                                  @Valid @RequestBody VisitCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(visitService.register(userId, request));
    }

    // 36. 방문 지정 가능한 면 (쿼리의 +09:00은 URL 인코딩해서 %2B09:00으로 보내야 함)
    @GetMapping("/available-slots")
    public List<AvailableSlotResponse> getAvailableSlots(
            @AuthenticationPrincipal Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startAt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endAt) {
        return visitService.getAvailableSlots(userId, startAt, endAt);
    }

    // 37. 우리 세대 방문 목록
    @GetMapping
    public VisitPageResponse getVisits(@AuthenticationPrincipal Long userId,
                                       @RequestParam VisitScope scope,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        return visitService.getVisits(userId, scope, page, size);
    }

    // 38. 방문 상세
    @GetMapping("/{visitId}")
    public VisitDetailResponse getVisit(@AuthenticationPrincipal Long userId, @PathVariable Long visitId) {
        return visitService.getVisit(userId, visitId);
    }

    // 39. 방문 수정 (시작 전)
    @PatchMapping("/{visitId}")
    public VisitResponse update(@AuthenticationPrincipal Long userId, @PathVariable Long visitId,
                                @Valid @RequestBody VisitUpdateRequest request) {
        return visitService.update(userId, visitId, request);
    }

    // 40. 방문 삭제 (시작 전)
    @DeleteMapping("/{visitId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Long userId, @PathVariable Long visitId) {
        visitService.delete(userId, visitId);
        return ResponseEntity.noContent().build();
    }

    // 41. [방문 종료]
    @PostMapping("/{visitId}/end")
    public VisitResponse end(@AuthenticationPrincipal Long userId, @PathVariable Long visitId) {
        return visitService.end(userId, visitId);
    }
}
