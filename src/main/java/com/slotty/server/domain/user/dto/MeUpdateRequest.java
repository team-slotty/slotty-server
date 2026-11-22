package com.slotty.server.domain.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// 6. 내 정보 수정: 보낸 필드만 바뀜 (모두 선택). 규칙은 회원가입과 같음
public record MeUpdateRequest(
        @Size(max = 30, message = "이름은 30자 이하입니다.")
        String name,

        @Pattern(regexp = "^01[0-9]-?\\d{3,4}-?\\d{4}$", message = "전화번호 형식이 올바르지 않습니다.")
        String phone,          // 010-1234-5678 또는 01012345678 → 숫자만 저장

        Boolean phonePublic    // 연락처 공개 (6-4 설정)
) {
}
