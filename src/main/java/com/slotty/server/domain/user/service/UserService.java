package com.slotty.server.domain.user.service;

import com.slotty.server.domain.slot.ParkingSlotRepository;
import com.slotty.server.domain.user.User;
import com.slotty.server.domain.user.UserRepository;
import com.slotty.server.domain.user.dto.MeResponse;
import com.slotty.server.domain.user.dto.MeResponse.Anchor;
import com.slotty.server.domain.user.dto.MeResponse.HouseholdInfo;
import com.slotty.server.domain.user.dto.MeResponse.VillaInfo;
import com.slotty.server.domain.villa.Household;
import com.slotty.server.domain.villa.Villa;
import com.slotty.server.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.slotty.server.global.exception.ErrorCode.UNAUTHORIZED;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final ParkingSlotRepository parkingSlotRepository;

    // 5. 내 정보: 앱 시작 시 화면 분기 (소속 전 → 시작 방식 선택, 소속 후 → 홈, isAdmin → 관리 탭)
    public MeResponse getMe(Long userId) {
        // 토큰은 유효한데 사용자가 없음 = 탈퇴한 계정 → 401 (앱은 로그인 화면으로)
        User user = userRepository.findActiveWithOptionalHousehold(userId)
                .orElseThrow(() -> new BusinessException(UNAUTHORIZED));

        Household household = user.getHousehold();
        if (household == null) { // 가입 직후: 세대·빌라 없음
            return new MeResponse(user.getId(), user.getLoginId(), user.getName(), user.getPhone(),
                    user.isPhonePublic(), false, null, null);
        }

        Villa villa = household.getVilla();
        // adminUser는 지연 로딩 프록시지만 getId()는 쿼리 없이 FK 값으로 바로 나옴
        boolean isAdmin = villa.getAdminUser() != null && villa.getAdminUser().getId().equals(user.getId());

        HouseholdInfo householdInfo = new HouseholdInfo(
                household.getId(),
                household.getUnitNumber(),
                parkingSlotRepository.findIdsByHouseholdId(household.getId())
        );
        Anchor anchor = (villa.getLatitude() == null || villa.getLongitude() == null)
                ? null
                : new Anchor(villa.getLatitude(), villa.getLongitude());
        VillaInfo villaInfo = new VillaInfo(villa.getId(), villa.getName(),
                villa.getLayoutTemplate(), villa.getEntranceSide(), anchor);

        return new MeResponse(user.getId(), user.getLoginId(), user.getName(), user.getPhone(),
                user.isPhonePublic(), isAdmin, householdInfo, villaInfo);
    }
}
