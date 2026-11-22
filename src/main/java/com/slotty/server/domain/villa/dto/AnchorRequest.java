package com.slotty.server.domain.villa.dto;

import com.slotty.server.domain.villa.AnchorMode;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AnchorRequest(
        @NotNull AnchorMode mode,                                           // ADDRESS | MANUAL
        @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,           // MANUAL일 때 필수
        @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude
) {
}
