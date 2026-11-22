package com.slotty.server.domain.availability.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record SkipRequest(@NotNull LocalDate skipDate) {
}
