package com.slotty.server.domain.vehicle.dto;

import com.slotty.server.domain.user.User;
import com.slotty.server.domain.vehicle.Vehicle;

public record VehicleResponse(
        Long vehicleId,
        String plateNumber,
        ContactUser contactUser // null이면 앱이 "담당자 지정 필요" 안내
) {
    public record ContactUser(Long userId, String name) {
    }

    public static VehicleResponse from(Vehicle vehicle) {
        User contact = vehicle.getContactUser();
        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getPlateNumber(),
                contact == null ? null : new ContactUser(contact.getId(), contact.getName())
        );
    }
}
