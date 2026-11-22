package com.slotty.server.global.external;

import java.math.BigDecimal;

// 위도·경도 (DB DECIMAL(10,7)에 맞춰 소수점 7자리)
public record Coordinates(BigDecimal latitude, BigDecimal longitude) {
}
