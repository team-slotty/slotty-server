package com.slotty.server.global.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 서비스에서 던진 비즈니스 예외 (ErrorCode에 상태·메시지가 들어 있음)
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        return toResponse(e.getErrorCode(), Map.of());
    }

    // @Valid 검증 실패 (@NotBlank, @NotNull 등) → 필드별 사유를 details에
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        Map<String, Object> details = new LinkedHashMap<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            details.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return toResponse(ErrorCode.VALIDATION_FAILED, details);
    }

    // JSON 문법 오류, 타입이 안 맞는 값, enum에 없는 값 (예: "method": "FOO")
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException e) {
        return toResponse(ErrorCode.VALIDATION_FAILED, Map.of("body", "요청 본문 형식이 올바르지 않습니다."));
    }

    // 쿼리 파라미터·경로 변수 타입 오류 (예: ?page=abc)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return toResponse(ErrorCode.VALIDATION_FAILED, Map.of(e.getName(), "형식이 올바르지 않습니다."));
    }

    private ResponseEntity<ErrorResponse> toResponse(ErrorCode errorCode, Map<String, Object> details) {
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode, details));
    }
}
