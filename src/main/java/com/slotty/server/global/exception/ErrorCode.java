package com.slotty.server.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

// enum: HttpStatus + Message
@Getter
public enum ErrorCode {

    NOT_IN_HOUSEHOLD(HttpStatus.FORBIDDEN, "세대에 소속된 사용자만 이용할 수 있습니다."), // 403 (누군지는 아는데 권한이 없음)
    INVALID_LOGIN(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."), // 401 (누구인지 확인 못 함)
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "로그인이 만료되었습니다. 다시 로그인해 주세요."),
    LOGIN_ID_DUPLICATED(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다."),

    // 세대 참여
    INVITE_CODE_INVALID(HttpStatus.NOT_FOUND, "초대 코드가 올바르지 않습니다."),
    ALREADY_IN_HOUSEHOLD(HttpStatus.CONFLICT, "이미 세대에 소속되어 있습니다."),
    TOO_MANY_ATTEMPTS(HttpStatus.TOO_MANY_REQUESTS, "시도 횟수를 초과했습니다. 잠시 후 다시 시도해 주세요."),

    // 400: 요청 값 형식·범위 오류 (details에 필드별 사유)
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    // 404: 없는 주소 (경로 오타 등)
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 주소를 찾을 수 없습니다."),

    // 입출차
    VEHICLE_NOT_FOUND(HttpStatus.NOT_FOUND, "차량을 찾을 수 없습니다."),
    SLOT_NOT_FOUND(HttpStatus.NOT_FOUND, "주차면을 찾을 수 없습니다."),
    SLOT_NOT_ALLOWED(HttpStatus.FORBIDDEN, "입차할 수 없는 주차면입니다."),
    SLOT_OCCUPIED(HttpStatus.CONFLICT, "이미 다른 차량이 주차된 면입니다."),
    NO_VEHICLE(HttpStatus.CONFLICT, "등록된 차량이 없습니다."),
    NOT_PARKED(HttpStatus.CONFLICT, "주차 중인 차량이 없습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

}
