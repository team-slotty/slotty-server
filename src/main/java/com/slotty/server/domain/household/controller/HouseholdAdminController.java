package com.slotty.server.domain.household.controller;

import com.slotty.server.domain.household.dto.HouseholdCreateRequest;
import com.slotty.server.domain.household.dto.HouseholdCreateResponse;
import com.slotty.server.domain.household.dto.HouseholdSummaryResponse;
import com.slotty.server.domain.household.dto.InviteCodeResponse;
import com.slotty.server.domain.household.service.HouseholdAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 관리자 전용 세대 관리 (입주민용 /join, /me는 HouseholdController)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/households")
public class HouseholdAdminController {

    private final HouseholdAdminService householdAdminService;

    @GetMapping
    public List<HouseholdSummaryResponse> getHouseholds(@AuthenticationPrincipal Long userId) {
        return householdAdminService.getHouseholds(userId);
    }

    @PostMapping
    public ResponseEntity<HouseholdCreateResponse> addHousehold(@AuthenticationPrincipal Long userId,
                                                                @Valid @RequestBody HouseholdCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(householdAdminService.addHousehold(userId, request));
    }

    @DeleteMapping("/{householdId}")
    public ResponseEntity<Void> deleteHousehold(@AuthenticationPrincipal Long userId, @PathVariable Long householdId) {
        householdAdminService.deleteHousehold(userId, householdId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{householdId}/invite-code/reissue")
    public InviteCodeResponse reissueInviteCode(@AuthenticationPrincipal Long userId, @PathVariable Long householdId) {
        return householdAdminService.reissueInviteCode(userId, householdId);
    }

    @DeleteMapping("/{householdId}/members/{memberId}")
    public ResponseEntity<Void> removeMember(@AuthenticationPrincipal Long userId,
                                             @PathVariable Long householdId,
                                             @PathVariable Long memberId) {
        householdAdminService.removeMember(userId, householdId, memberId);
        return ResponseEntity.noContent().build();
    }
}
