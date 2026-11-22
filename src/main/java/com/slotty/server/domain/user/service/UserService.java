package com.slotty.server.domain.user.service;

import com.slotty.server.domain.auth.service.AuthService;
import com.slotty.server.domain.slot.ParkingSlotRepository;
import com.slotty.server.domain.user.User;
import com.slotty.server.domain.user.UserRepository;
import com.slotty.server.domain.user.dto.MeResponse;
import com.slotty.server.domain.user.dto.MeResponse.Anchor;
import com.slotty.server.domain.user.dto.MeResponse.HouseholdInfo;
import com.slotty.server.domain.user.dto.MeResponse.VillaInfo;
import com.slotty.server.domain.user.dto.MeUpdateRequest;
import com.slotty.server.domain.vehicle.VehicleRepository;
import com.slotty.server.domain.villa.Household;
import com.slotty.server.domain.villa.Villa;
import com.slotty.server.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static com.slotty.server.global.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final ParkingSlotRepository parkingSlotRepository;
    private final VehicleRepository vehicleRepository;
    private final AuthService authService;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

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

    // ===== 6. 내 정보 수정 (이름·전화번호·연락처 공개) =====
    @Transactional
    public MeResponse updateMe(Long userId, MeUpdateRequest request) {
        User user = userRepository.findActiveWithOptionalHousehold(userId)
                .orElseThrow(() -> new BusinessException(UNAUTHORIZED));

        String name = request.name() == null ? null : request.name().strip();
        if (name != null && name.isEmpty()) {
            throw new BusinessException(VALIDATION_FAILED); // 공백만 보낸 이름 (NOT NULL 컬럼에 빈 이름이 들어가지 않게)
        }
        String phone = request.phone() == null ? null : request.phone().replace("-", ""); // 가입과 같이 숫자만

        user.updateProfile(name, phone, request.phonePublic()); // dirty checking으로 UPDATE
        return getMe(userId); // 응답은 GET /me와 같음 (명세)
    }

    // ===== 7. 탈퇴 =====
    @Transactional
    public void withdraw(Long userId) {
        User user = userRepository.findActiveWithOptionalHousehold(userId)
                .orElseThrow(() -> new BusinessException(UNAUTHORIZED)); // 이미 탈퇴

        // 관리자가 빠지면 빌라에 관리자가 없어짐 → 먼저 13번으로 위임
        if (user.getHousehold() != null) {
            User admin = user.getHousehold().getVilla().getAdminUser();
            if (admin != null && admin.getId().equals(user.getId())) {
                throw new BusinessException(ADMIN_MUST_DELEGATE);
            }
        }

        vehicleRepository.clearContactUser(user.getId()); // 내가 연락 담당자인 차량 → 담당자 없음 (세대원 내보내기와 같음)
        user.withdraw(LocalDateTime.now(KST).truncatedTo(ChronoUnit.SECONDS));
        authService.revokeAllRefreshTokens(user.getId()); // 모든 기기 자동 로그인 차단
    }
}
