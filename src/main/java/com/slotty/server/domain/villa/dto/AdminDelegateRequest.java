package com.slotty.server.domain.villa.dto;

import jakarta.validation.constraints.NotNull;

public record AdminDelegateRequest(@NotNull Long userId) {
}
