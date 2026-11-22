package com.slotty.server.domain.slot.controller;

import com.slotty.server.domain.slot.dto.SlotDetailResponse;
import com.slotty.server.domain.slot.service.SlotService;
import com.slotty.server.domain.slot.dto.SlotAssignmentRequest;
import com.slotty.server.domain.slot.service.SlotAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/parking-slots")
public class SlotController {

    private final SlotService slotService;
    private final SlotAssignmentService slotAssignmentService;

    @GetMapping("/{slotId}")
    public SlotDetailResponse getSlotDetail(@AuthenticationPrincipal Long userId, @PathVariable Long slotId) {
        return slotService.getSlotDetail(userId, slotId);
    }

    // 21. 세대별 면 배정 (관리자)
    @PutMapping("/assignments")
    public ResponseEntity<Void> assign(@AuthenticationPrincipal Long userId,
                                       @Valid @RequestBody SlotAssignmentRequest request) {
        slotAssignmentService.assign(userId, request);
        return ResponseEntity.noContent().build();
    }
}
