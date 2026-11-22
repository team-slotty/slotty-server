package com.slotty.server.domain.household.service;

import com.slotty.server.domain.availability.AvailabilityExceptionRepository;
import com.slotty.server.domain.availability.AvailabilityRepository;
import com.slotty.server.domain.household.dto.HouseholdCreateRequest;
import com.slotty.server.domain.household.dto.HouseholdCreateResponse;
import com.slotty.server.domain.household.dto.HouseholdSummaryResponse;
import com.slotty.server.domain.household.dto.HouseholdSummaryResponse.Member;
import com.slotty.server.domain.household.dto.InviteCodeResponse;
import com.slotty.server.domain.slot.ParkingSlot;
import com.slotty.server.domain.slot.ParkingSlotRepository;
import com.slotty.server.domain.user.User;
import com.slotty.server.domain.user.UserRepository;
import com.slotty.server.domain.vehicle.VehicleRepository;
import com.slotty.server.domain.villa.AdminChecker;
import com.slotty.server.domain.villa.Household;
import com.slotty.server.domain.villa.HouseholdRepository;
import com.slotty.server.domain.villa.InviteCodeGenerator;
import com.slotty.server.domain.villa.Villa;
import com.slotty.server.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.slotty.server.global.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HouseholdAdminService {

    private final AdminChecker adminChecker;
    private final InviteCodeGenerator inviteCodeGenerator;
    private final HouseholdRepository householdRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final ParkingSlotRepository parkingSlotRepository;
    private final AvailabilityRepository availabilityRepository;
    private final AvailabilityExceptionRepository availabilityExceptionRepository;

    // ===== 14. 세대 목록 =====
    public List<HouseholdSummaryResponse> getHouseholds(Long userId) {
        User admin = adminChecker.requireAdmin(userId);
        Villa villa = admin.getHousehold().getVilla();

        // 세대·면·차량 수·세대원을 각각 빌라 단위로 한 번씩 조회 → 세대가 늘어도 쿼리 4번
        List<Household> households = householdRepository.findByVillaIdOrderById(villa.getId());

        Map<Long, List<String>> slotLabelsByHousehold = parkingSlotRepository.findAllWithHouseholdByVillaId(villa.getId())
                .stream()
                .filter(slot -> slot.getHousehold() != null)
                .collect(Collectors.groupingBy(slot -> slot.getHousehold().getId(),
                        Collectors.mapping(ParkingSlot::getLabel, Collectors.toList())));

        Map<Long, Long> vehicleCountByHousehold = vehicleRepository.countByHouseholdInVilla(villa.getId())
                .stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

        Map<Long, List<Member>> membersByHousehold = userRepository.findActiveMembersByVillaId(villa.getId())
                .stream()
                .collect(Collectors.groupingBy(member -> member.getHousehold().getId(),
                        Collectors.mapping(member -> new Member(member.getId(), member.getName(),
                                member.getId().equals(admin.getId())), Collectors.toList())));

        return households.stream()
                .map(h -> new HouseholdSummaryResponse(
                        h.getId(),
                        h.getUnitNumber(),
                        h.getInviteCode(),
                        slotLabelsByHousehold.getOrDefault(h.getId(), List.of()),
                        vehicleCountByHousehold.getOrDefault(h.getId(), 0L),
                        membersByHousehold.getOrDefault(h.getId(), List.of())))
                .toList();
    }

    // ===== 15. 세대 추가 =====
    @Transactional
    public HouseholdCreateResponse addHousehold(Long userId, HouseholdCreateRequest request) {
        Villa villa = adminChecker.requireAdmin(userId).getHousehold().getVilla();
        String unitNumber = request.unitNumber().strip();

        if (householdRepository.existsByVillaIdAndUnitNumber(villa.getId(), unitNumber)) {
            throw new BusinessException(UNIT_NUMBER_DUPLICATED);
        }
        Household household = Household.create(villa, unitNumber, inviteCodeGenerator.generate());
        try {
            householdRepository.saveAndFlush(household); // 동시 추가 시 uk_household_villa_unit 위반
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(UNIT_NUMBER_DUPLICATED);
        }
        return new HouseholdCreateResponse(household.getId(), household.getUnitNumber(), household.getInviteCode());
    }

    // ===== 16. 세대 삭제 =====
    @Transactional
    public void deleteHousehold(Long userId, Long householdId) {
        Villa villa = adminChecker.requireAdmin(userId).getHousehold().getVilla();
        Household household = findHouseholdInVilla(householdId, villa);

        // 가입자(탈퇴하면 household_id가 NULL이 되므로 남아 있으면 현재 세대원)나 차량이 있으면 삭제 불가
        if (userRepository.existsByHouseholdId(household.getId())
                || vehicleRepository.existsByHouseholdId(household.getId())) {
            throw new BusinessException(HOUSEHOLD_NOT_EMPTY);
        }

        // 배정돼 있던 면은 공용으로, 그 면의 가용시간은 삭제 (공용 면에는 가용시간이 없음)
        for (ParkingSlot slot : parkingSlotRepository.findByHouseholdIdOrderById(household.getId())) {
            deleteAvailabilities(slot.getId());
            slot.assignTo(null);
        }

        try {
            householdRepository.delete(household);
            householdRepository.flush(); // 방문·자주 오는 차 기록(FK)이 남아 있으면 여기서 위반 → 409
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(HOUSEHOLD_NOT_EMPTY);
        }
    }

    // ===== 17. 초대 코드 재발급 =====
    @Transactional
    public InviteCodeResponse reissueInviteCode(Long userId, Long householdId) {
        Villa villa = adminChecker.requireAdmin(userId).getHousehold().getVilla();
        Household household = findHouseholdInVilla(householdId, villa);

        household.changeInviteCode(inviteCodeGenerator.generate()); // 같은 행을 덮어써 이전 코드는 즉시 무효
        return new InviteCodeResponse(household.getInviteCode());
    }

    // ===== 18. 세대원 내보내기 =====
    @Transactional
    public void removeMember(Long userId, Long householdId, Long memberId) {
        User admin = adminChecker.requireAdmin(userId);
        Household household = findHouseholdInVilla(householdId, admin.getHousehold().getVilla());

        User member = userRepository.findByIdAndHouseholdIdAndDeletedAtIsNull(memberId, household.getId())
                .orElseThrow(() -> new BusinessException(USER_NOT_FOUND));
        if (member.getId().equals(admin.getId())) {
            throw new BusinessException(CANNOT_REMOVE_ADMIN); // 관리자 본인은 먼저 위임(13)
        }

        member.leaveHousehold();                               // 계정은 유지, 다음 실행 때 시작 화면(0-4)으로
        vehicleRepository.clearContactUser(member.getId());    // 이 사람이 연락 담당자인 차량 → 담당자 없음
    }

    // ===== 공통 =====

    // 다른 빌라 세대면 404 (존재 여부를 숨김)
    private Household findHouseholdInVilla(Long householdId, Villa villa) {
        return householdRepository.findByIdAndVillaId(householdId, villa.getId())
                .orElseThrow(() -> new BusinessException(HOUSEHOLD_NOT_FOUND));
    }

    private void deleteAvailabilities(Long slotId) {
        availabilityExceptionRepository.deleteAllBySlotId(slotId); // FK 때문에 예외 먼저
        availabilityRepository.deleteAllBySlotId(slotId);
    }
}
