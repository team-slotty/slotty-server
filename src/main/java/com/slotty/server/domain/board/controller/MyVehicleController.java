package com.slotty.server.domain.board.controller;

import com.slotty.server.domain.board.dto.MyVehiclesResponse;
import com.slotty.server.domain.board.service.MyVehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MyVehicleController {

    private final MyVehicleService myVehicleService;

    @GetMapping("/api/vehicles/me")
    public MyVehiclesResponse getMyVehicles() {
        return myVehicleService.getMyVehicles(1L);
    }
}
