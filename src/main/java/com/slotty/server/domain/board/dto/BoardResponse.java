package com.slotty.server.domain.board.dto;

import java.time.LocalDateTime;
import java.util.List;

public record BoardResponse(
        String villaName,
        List<Slot> slots
) {
    public record Slot(
            int number,
            String assignedUnit,
            String status,
            Occupant occupant
    ){}

    public record Occupant(
            String plateNumber,
            LocalDateTime enteredAt
    ){}
}
