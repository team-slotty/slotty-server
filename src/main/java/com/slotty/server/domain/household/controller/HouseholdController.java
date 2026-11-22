package com.slotty.server.domain.household.controller;

import com.slotty.server.domain.household.dto.JoinRequest;
import com.slotty.server.domain.household.dto.JoinResponse;
import com.slotty.server.domain.household.dto.MyHouseholdResponse;
import com.slotty.server.domain.household.service.HouseholdService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/households")
public class HouseholdController {

    private final HouseholdService householdService;

    @PostMapping("/join")
    public JoinResponse join(@AuthenticationPrincipal Long userId, @Valid @RequestBody JoinRequest request) {
        return householdService.join(userId, request);
    }

    @GetMapping("/me")
    public MyHouseholdResponse getMyHousehold(@AuthenticationPrincipal Long userId) {
        return householdService.getMyHousehold(userId);
    }
}
