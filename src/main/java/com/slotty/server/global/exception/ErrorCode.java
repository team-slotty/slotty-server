package com.slotty.server.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

// enum: HttpStatus + Message
@Getter
public enum ErrorCode {

    NOT_IN_HOUSEHOLD(HttpStatus.FORBIDDEN, "세대에 소속된 사용자만 이용할 수 있습니다."), // 403 (누군지는 아는데 권한이 없음)
    INVALID_LOGIN(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."), // 401 (누구인지 확인 못 함)
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "로그인이 만료되었습니다. 다시 로그인해 주세요.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

}
