package com.slotty.server.domain.availability.dto;

import java.time.LocalDate;

public record SkipResponse(Long availabilityId, LocalDate skipDate) {
}
