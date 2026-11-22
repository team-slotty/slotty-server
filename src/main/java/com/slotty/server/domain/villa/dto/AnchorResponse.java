package com.slotty.server.domain.villa.dto;

import com.slotty.server.domain.villa.Villa;

import java.math.BigDecimal;

// 빌라 기준점 (좌표가 없으면 응답에서 anchor 자체가 null)
public record AnchorResponse(BigDecimal latitude, BigDecimal longitude) {

    public static AnchorResponse of(Villa villa) {
        if (villa.getLatitude() == null || villa.getLongitude() == null) {
            return null;
        }
        return new AnchorResponse(villa.getLatitude(), villa.getLongitude());
    }
}
