package com.slotty.server.domain.slot.controller;

import com.slotty.server.domain.slot.dto.SlotDetailResponse;
import com.slotty.server.domain.slot.service.SlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/parking-slots")
public class SlotController {

    private final SlotService slotService;

    @GetMapping("/{slotId}")
    public SlotDetailResponse getSlotDetail(@AuthenticationPrincipal Long userId, @PathVariable Long slotId) {
        return slotService.getSlotDetail(userId, slotId);
    }
}
