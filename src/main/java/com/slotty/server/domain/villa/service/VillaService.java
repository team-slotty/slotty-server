package com.slotty.server.domain.villa.service;

import com.slotty.server.domain.user.User;
import com.slotty.server.domain.user.UserRepository;
import com.slotty.server.domain.villa.AdminChecker;
import com.slotty.server.domain.villa.Villa;
import com.slotty.server.domain.villa.dto.AdminDelegateRequest;
import com.slotty.server.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.slotty.server.global.exception.ErrorCode.USER_NOT_FOUND;
import static com.slotty.server.global.exception.ErrorCode.VALIDATION_FAILED;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VillaService {

    private final AdminChecker adminChecker;
    private final UserRepository userRepository;

    // ===== 13. 관리자 위임 =====
    @Transactional
    public void delegateAdmin(Long userId, AdminDelegateRequest request) {
        User admin = adminChecker.requireAdmin(userId);
        Villa villa = admin.getHousehold().getVilla();

        if (request.userId().equals(admin.getId())) {
            throw new BusinessException(VALIDATION_FAILED); // 본인에게 위임
        }
        // 같은 빌라에 소속된 활성 사용자만 (다른 빌라·미소속·탈퇴 사용자는 404)
        User target = userRepository.findActiveWithHouseholdAndVilla(request.userId())
                .filter(u -> u.getHousehold().getVilla().getId().equals(villa.getId()))
                .orElseThrow(() -> new BusinessException(USER_NOT_FOUND));

        villa.changeAdmin(target); // 이후 나는 일반 입주민
    }
}
