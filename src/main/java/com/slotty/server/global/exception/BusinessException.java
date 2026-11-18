package com.slotty.server.global.exception;

import lombok.Getter;

// ErrorCode를 담는 RuntimeException
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
