package com.slotty.server.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank(message = "아이디를 입력해 주세요.")
        @Pattern(regexp = "^[A-Za-z0-9]{4,30}$", message = "아이디는 영문·숫자 4~30자입니다.")
        String loginId,

        @NotBlank(message = "비밀번호를 입력해 주세요.")
        @Size(min = 8, max = 72, message = "비밀번호는 8~72자입니다.") // BCrypt는 72바이트까지만 사용
        String password,

        @NotBlank(message = "이름을 입력해 주세요.")
        @Size(max = 30, message = "이름은 30자 이하입니다.")
        String name,

        @NotBlank(message = "전화번호를 입력해 주세요.")
        @Pattern(regexp = "^01[0-9]-?\\d{3,4}-?\\d{4}$", message = "전화번호 형식이 올바르지 않습니다.")
        String phone,                 // 010-1234-5678 또는 01012345678 → 숫자만 저장

        @NotNull(message = "연락처 공개 여부를 선택해 주세요.")
        Boolean phonePublic
) {
}
