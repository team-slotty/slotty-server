package com.slotty.server.domain.availability.controller;

import com.slotty.server.domain.availability.dto.AvailabilityRequest;
import com.slotty.server.domain.availability.dto.AvailabilityResponse;
import com.slotty.server.domain.availability.dto.SkipRequest;
import com.slotty.server.domain.availability.dto.SkipResponse;
import com.slotty.server.domain.availability.service.AvailabilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/availabilities")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @GetMapping
    public List<AvailabilityResponse> getAvailabilities(@AuthenticationPrincipal Long userId,
                                                        @RequestParam(required = false) Long slotId) {
        return availabilityService.getAvailabilities(userId, slotId);
    }

    @PostMapping
    public ResponseEntity<AvailabilityResponse> register(@AuthenticationPrincipal Long userId,
                                                         @Valid @RequestBody AvailabilityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(availabilityService.register(userId, request));
    }

    @DeleteMapping("/{availabilityId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Long userId, @PathVariable Long availabilityId) {
        availabilityService.delete(userId, availabilityId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{availabilityId}/exceptions")
    public ResponseEntity<SkipResponse> skip(@AuthenticationPrincipal Long userId,
                                             @PathVariable Long availabilityId,
                                             @Valid @RequestBody SkipRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(availabilityService.skip(userId, availabilityId, request));
    }
}
