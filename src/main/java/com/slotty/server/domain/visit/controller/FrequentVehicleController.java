package com.slotty.server.domain.visit.controller;

import com.slotty.server.domain.visit.dto.FrequentVehicleResponse;
import com.slotty.server.domain.visit.service.VisitService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/frequent-vehicles")
public class FrequentVehicleController {

    private final VisitService visitService;

    // 42. 자주 오는 방문차 (최근 5대)
    @GetMapping
    public List<FrequentVehicleResponse> getFrequentVehicles(@AuthenticationPrincipal Long userId) {
        return visitService.getFrequentVehicles(userId);
    }
}
