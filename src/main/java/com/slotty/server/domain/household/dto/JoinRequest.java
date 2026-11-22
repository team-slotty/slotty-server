package com.slotty.server.domain.household.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinRequest(
        @NotBlank(message = "초대 코드를 입력해 주세요.")
        @Size(max = 20, message = "초대 코드가 올바르지 않습니다.") // 코드는 12자 이하, 앞뒤 공백까지 여유
        String inviteCode
) {
}
