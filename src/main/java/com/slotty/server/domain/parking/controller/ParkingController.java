package com.slotty.server.domain.parking.controller;

import com.slotty.server.domain.parking.dto.CheckInOptionsResponse;
import com.slotty.server.domain.parking.dto.CheckInRequest;
import com.slotty.server.domain.parking.dto.CheckInResponse;
import com.slotty.server.domain.parking.dto.CheckInResult;
import com.slotty.server.domain.parking.service.ParkingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/parking")
public class ParkingController {

    private final ParkingService parkingService;

    @GetMapping("/check-in-options")
    public CheckInOptionsResponse getCheckInOptions(@AuthenticationPrincipal Long userId) {
        return parkingService.getCheckInOptions(userId);
    }

    @PostMapping("/check-in")
    public ResponseEntity<CheckInResponse> checkIn(@AuthenticationPrincipal Long userId,
                                                   @Valid @RequestBody CheckInRequest request) {
        CheckInResult result = parkingService.checkIn(userId, request);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.response());
    }
}
