package com.slotty.server.domain.vehicle.controller;

import com.slotty.server.domain.vehicle.dto.VehicleCreateRequest;
import com.slotty.server.domain.vehicle.dto.VehicleResponse;
import com.slotty.server.domain.vehicle.dto.VehicleUpdateRequest;
import com.slotty.server.domain.vehicle.service.VehicleService;
import com.slotty.server.domain.vehicle.dto.VehicleSearchResponse;
import com.slotty.server.domain.vehicle.service.VehicleSearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;
    private final VehicleSearchService vehicleSearchService;

    @GetMapping
    public List<VehicleResponse> getVehicles(@AuthenticationPrincipal Long userId) {
        return vehicleService.getVehicles(userId);
    }

    @PostMapping
    public ResponseEntity<VehicleResponse> register(@AuthenticationPrincipal Long userId,
                                                    @Valid @RequestBody VehicleCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicleService.register(userId, request));
    }

    @PatchMapping("/{vehicleId}")
    public VehicleResponse update(@AuthenticationPrincipal Long userId,
                                  @PathVariable Long vehicleId,
                                  @Valid @RequestBody VehicleUpdateRequest request) {
        return vehicleService.update(userId, vehicleId, request);
    }

    @DeleteMapping("/{vehicleId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Long userId, @PathVariable Long vehicleId) {
        vehicleService.delete(userId, vehicleId);
        return ResponseEntity.noContent().build(); // 204
    }

    @GetMapping("/search")
    public VehicleSearchResponse search(@AuthenticationPrincipal Long userId, @RequestParam String plate) {
        return vehicleSearchService.search(userId, plate);
    }
}
