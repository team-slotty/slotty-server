package com.slotty.server.domain.parking.controller;

import com.slotty.server.domain.parking.dto.*;
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
import org.springframework.web.bind.annotation.RequestParam;

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

    @GetMapping("/check-out-options")
    public CheckOutOptionsResponse getCheckOutOptions(@AuthenticationPrincipal Long userId) {
        return parkingService.getCheckOutOptions(userId);
    }

    @PostMapping("/check-out")
    public CheckOutResponse checkOut(@AuthenticationPrincipal Long userId,
                                     @Valid @RequestBody CheckOutRequest request) {
        return parkingService.checkOut(userId, request);
    }

    @GetMapping("/history")
    public HistoryResponse getHistory(@AuthenticationPrincipal Long userId,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        return parkingService.getHistory(userId, page, size);
    }
}
