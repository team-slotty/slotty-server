package com.slotty.server.domain.villa;

import com.slotty.server.domain.user.User;
import com.slotty.server.domain.user.UserRepository;
import com.slotty.server.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.slotty.server.global.exception.ErrorCode.FORBIDDEN;
import static com.slotty.server.global.exception.ErrorCode.NOT_IN_HOUSEHOLD;

// 관리자 전용 API의 공통 입구: 세대 소속(403 NOT_IN_HOUSEHOLD) + 빌라 관리자(403 FORBIDDEN) 확인
@Component
@RequiredArgsConstructor
public class AdminChecker {

    private final UserRepository userRepository;

    // 통과하면 사용자(세대·빌라 포함)를 돌려줌
    public User requireAdmin(Long userId) {
        User user = userRepository.findActiveWithHouseholdAndVilla(userId)
                .orElseThrow(() -> new BusinessException(NOT_IN_HOUSEHOLD));
        User admin = user.getHousehold().getVilla().getAdminUser();
        if (admin == null || !admin.getId().equals(user.getId())) {
            throw new BusinessException(FORBIDDEN);
        }
        return user;
    }
}
