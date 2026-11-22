package com.slotty.server.domain.slot;

import java.math.BigDecimal;

// 배치도에서 면 하나의 위치 (중심 x·y, 너비 w, 높이 h는 0~1 비율, 회전 각도)
public record SlotPosition(BigDecimal x, BigDecimal y, BigDecimal w, BigDecimal h, short rotation) {
}
