package com.slotty.server.domain.villa;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

// 세대 초대 코드: 10자리 대문자·숫자 (헷갈리는 0/O, 1/I/L 제외)
@Component
@RequiredArgsConstructor
public class InviteCodeGenerator {

    private static final String CHARACTERS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"; // 31자
    private static final int LENGTH = 10;                                         // 31^10 ≈ 8.2 × 10^14 가지
    private static final SecureRandom RANDOM = new SecureRandom();                // 예측 불가능한 난수 (Random은 예측 가능)

    private final HouseholdRepository householdRepository;

    // 이미 쓰는 코드와 겹치면 다시 만듦 (확률은 매우 낮지만 uk_household_invite_code 위반 방지)
    public String generate() {
        String code;
        do {
            StringBuilder builder = new StringBuilder(LENGTH);
            for (int i = 0; i < LENGTH; i++) {
                builder.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
            }
            code = builder.toString();
        } while (householdRepository.existsByInviteCode(code));
        return code;
    }
}
