package com.slotty.server.domain.visit.dto;

import com.slotty.server.domain.visit.FrequentVehicle;

import java.time.LocalDateTime;

// 42. 자주 오는 방문차
public record FrequentVehicleResponse(String plateNumber, LocalDateTime lastVisitedAt) {

    public static FrequentVehicleResponse from(FrequentVehicle frequentVehicle) {
        return new FrequentVehicleResponse(frequentVehicle.getPlateNumber(), frequentVehicle.getLastVisitedAt());
    }
}
