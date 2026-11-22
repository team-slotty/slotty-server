package com.slotty.server.domain.unidentified;

// 46. 이력 필터
public enum ReportStatus {
    ALL, OPEN, RESOLVED;

    // 쿼리 조건: null=전체, true=미해제, false=해제
    public Boolean toOpenFlag() {
        return switch (this) {
            case ALL -> null;
            case OPEN -> true;
            case RESOLVED -> false;
        };
    }
}
