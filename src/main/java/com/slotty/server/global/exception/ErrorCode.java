package com.slotty.server.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

// enum: HttpStatus + Message
@Getter
public enum ErrorCode {

    NOT_IN_HOUSEHOLD(HttpStatus.FORBIDDEN, "세대에 소속된 사용자만 이용할 수 있습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

}
