package com.slotty.server.global.exception;

import java.util.Map;

// record(code, message, details)
public record ErrorResponse(String code, String message, Map<String, Object> details) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return of(errorCode, Map.of());
    }

    // 검증 실패처럼 필드별 사유를 함께 내려줄 때 사용
    public static ErrorResponse of(ErrorCode errorCode, Map<String, Object> details) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), details);
    }
}
