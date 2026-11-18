package com.slotty.server.global.exception;

import java.util.Map;

// record(code, message, details)
public record ErrorResponse(String code, String message, Map<String, Object> details) {

    public static ErrorResponse of(ErrorCode errorCode) {

        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), Map.of());
    }
}
