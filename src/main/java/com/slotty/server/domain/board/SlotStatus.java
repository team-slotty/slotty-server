package com.slotty.server.domain.board;

// 면 상태(위에서부터 우선순위 순서대로. 판단 로직은 서비스의 if 순서로 하고 ordinal(순서)은 쓰지 않음)
public enum SlotStatus {
    UNIDENTIFIED,
    OCCUPIED,
    NEEDS_CHECK,
    VISITING,
    VISIT_SCHEDULED,
    AVAILABLE,
    EMPTY
}
