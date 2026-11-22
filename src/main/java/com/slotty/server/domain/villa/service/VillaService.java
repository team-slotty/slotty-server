package com.slotty.server.domain.villa.service;

import com.slotty.server.domain.availability.AvailabilityRepository;
import com.slotty.server.domain.parking.ParkingRecordRepository;
import com.slotty.server.domain.slot.ParkingSlot;
import com.slotty.server.domain.slot.ParkingSlotRepository;
import com.slotty.server.domain.slot.SlotLayoutCalculator;
import com.slotty.server.domain.slot.SlotPosition;
import com.slotty.server.domain.unidentified.UnidentifiedReportRepository;
import com.slotty.server.domain.user.User;
import com.slotty.server.domain.user.UserRepository;
import com.slotty.server.domain.villa.*;
import com.slotty.server.domain.villa.dto.*;
import com.slotty.server.domain.villa.dto.VillaCreateRequest.AssignmentInput;
import com.slotty.server.domain.villa.dto.VillaCreateResponse.HouseholdCode;
import com.slotty.server.domain.villa.dto.VillaCreateResponse.VillaSummary;
import com.slotty.server.global.exception.BusinessException;
import com.slotty.server.global.external.Coordinates;
import com.slotty.server.global.external.KakaoGeocoder;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.slotty.server.global.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VillaService {

    private final AdminChecker adminChecker;
    private final InviteCodeGenerator inviteCodeGenerator;
    private final KakaoGeocoder kakaoGeocoder;
    private final UserRepository userRepository;
    private final VillaRepository villaRepository;
    private final HouseholdRepository householdRepository;
    private final ParkingSlotRepository parkingSlotRepository;
    private final ParkingRecordRepository parkingRecordRepository;
    private final AvailabilityRepository availabilityRepository;
    private final UnidentifiedReportRepository unidentifiedReportRepository;

    // ===== 8. 빌라 온보딩 (빌라 → 세대 → 면 → 배정 → 내 소속 → 관리자, 한 트랜잭션) =====
    @Transactional
    public VillaCreateResponse createVilla(Long userId, VillaCreateRequest request) {
        User user = userRepository.findActiveWithOptionalHousehold(userId)
                .orElseThrow(() -> new BusinessException(UNAUTHORIZED));
        if (user.getHousehold() != null) {
            throw new BusinessException(ALREADY_IN_HOUSEHOLD);
        }

        // 1. 입력 검사: 호수 중복 409, 내 호수·배정 대상이 목록에 없으면 400
        List<String> unitNumbers = request.households().stream().map(h -> h.unitNumber().strip()).toList();
        if (new HashSet<>(unitNumbers).size() != unitNumbers.size()) {
            throw new BusinessException(UNIT_NUMBER_DUPLICATED);
        }
        String myUnitNumber = request.myUnitNumber().strip();
        if (!unitNumbers.contains(myUnitNumber)) {
            throw new BusinessException(VALIDATION_FAILED);
        }
        List<AssignmentInput> assignments = request.assignments() == null ? List.of() : request.assignments();
        for (AssignmentInput a : assignments) {
            if (!unitNumbers.contains(a.unitNumber().strip()) || labelIndex(a.slotLabel(), request.slotCount()) < 0) {
                throw new BusinessException(VALIDATION_FAILED);
            }
        }

        // 2. 빌라 (주소 → 좌표, 실패해도 생성은 계속)
        Optional<Coordinates> coordinates = kakaoGeocoder.geocode(request.address().strip());
        Villa villa = villaRepository.save(Villa.create(request.name().strip(), request.address().strip(),
                coordinates.orElse(null), request.layoutTemplate(), request.entranceSide()));

        // 3. 세대 (세대마다 초대 코드)
        Map<String, Household> householdByUnit = new LinkedHashMap<>();
        for (String unitNumber : unitNumbers) {
            householdByUnit.put(unitNumber,
                    householdRepository.save(Household.create(villa, unitNumber, inviteCodeGenerator.generate())));
        }

        // 4. 면 (템플릿 좌표 자동 계산) + 배정 (없는 면은 공용)
        Map<String, String> unitBySlotLabel = assignments.stream()
                .collect(Collectors.toMap(a -> a.slotLabel().strip(), a -> a.unitNumber().strip(), (first, second) -> second));
        List<SlotPosition> positions = SlotLayoutCalculator.calculate(
                request.layoutTemplate(), request.entranceSide(), request.slotCount());
        List<ParkingSlot> slots = new ArrayList<>();
        for (int i = 0; i < positions.size(); i++) {
            String label = labelOf(i);
            String unit = unitBySlotLabel.get(label);
            slots.add(parkingSlotRepository.save(ParkingSlot.create(villa,
                    unit == null ? null : householdByUnit.get(unit), label, positions.get(i))));
        }

        // 5. 내 소속 → 6. 관리자 지정 (villa ↔ users 순환 참조라 빌라를 먼저 만들고 마지막에 지정)
        user.joinHousehold(householdByUnit.get(myUnitNumber));
        villa.changeAdmin(user);

        return new VillaCreateResponse(
                new VillaSummary(villa.getId(), villa.getName(), AnchorResponse.of(villa)),
                coordinates.isEmpty(),
                slots.stream().map(SlotLayoutResponse::from).toList(),
                householdByUnit.values().stream()
                        .map(h -> new HouseholdCode(h.getId(), h.getUnitNumber(), h.getInviteCode())).toList());
    }

    // ===== 9. 빌라 정보 (입주민) =====
    public VillaResponse getMyVilla(Long userId) {
        User user = userRepository.findActiveWithHouseholdAndVilla(userId)
                .orElseThrow(() -> new BusinessException(NOT_IN_HOUSEHOLD));
        Villa villa = user.getHousehold().getVilla();
        return new VillaResponse(villa.getId(), villa.getName(), villa.getAddress(),
                villa.getLayoutTemplate(), villa.getEntranceSide(),
                parkingSlotRepository.countByVillaId(villa.getId()),
                householdRepository.countByVillaId(villa.getId()),
                AnchorResponse.of(villa));
    }

    // ===== 10. 빌라 이름·주소 수정 (관리자) =====
    @Transactional
    public VillaUpdateResponse updateVilla(Long userId, VillaUpdateRequest request) {
        Villa villa = adminChecker.requireAdmin(userId).getHousehold().getVilla();
        boolean geocodeFailed = false;

        if (request.name() != null) {
            villa.changeName(request.name().strip());
        }
        if (request.address() != null && !request.address().strip().equals(villa.getAddress())) {
            // 주소가 바뀌면 기준점도 새 주소로 (이전 현장 보정 값은 옛 주소 기준이라 덮어씀, 실패하면 null)
            Optional<Coordinates> coordinates = kakaoGeocoder.geocode(request.address().strip());
            villa.changeAddress(request.address().strip(), coordinates.orElse(null));
            geocodeFailed = coordinates.isEmpty();
        }
        return new VillaUpdateResponse(villa.getId(), villa.getName(), villa.getAddress(),
                AnchorResponse.of(villa), geocodeFailed);
    }

    // ===== 11. 기준점 재설정·현장 보정 (관리자) =====
    @Transactional
    public AnchorUpdateResponse updateAnchor(Long userId, AnchorRequest request) {
        Villa villa = adminChecker.requireAdmin(userId).getHousehold().getVilla();

        if (request.mode() == AnchorMode.MANUAL) {
            if (request.latitude() == null || request.longitude() == null) {
                throw new BusinessException(VALIDATION_FAILED);
            }
            villa.changeAnchor(new Coordinates(request.latitude(), request.longitude()));
            return new AnchorUpdateResponse(AnchorResponse.of(villa), false);
        }

        // ADDRESS: 다시 변환. 실패하면 기존 기준점은 그대로 둠 (현장 보정 값을 잃지 않게)
        Optional<Coordinates> coordinates = kakaoGeocoder.geocode(villa.getAddress());
        coordinates.ifPresent(villa::changeAnchor);
        return new AnchorUpdateResponse(AnchorResponse.of(villa), coordinates.isEmpty());
    }

    // ===== 12. 배치 템플릿·입구·면 수 변경 (관리자) =====
    @Transactional
    public LayoutResponse changeLayout(Long userId, LayoutRequest request) {
        Villa villa = adminChecker.requireAdmin(userId).getHousehold().getVilla();
        List<ParkingSlot> slots = new ArrayList<>(parkingSlotRepository.findAllWithHouseholdByVillaId(villa.getId())); // id 순
        int newCount = request.slotCount();

        // 줄이면 번호가 큰 면부터 삭제. 기록이 남은 면은 삭제할 수 없음 → 409
        List<ParkingSlot> toDelete = slots.size() > newCount ? slots.subList(newCount, slots.size()) : List.of();
        for (ParkingSlot slot : toDelete) {
            if (parkingRecordRepository.existsBySlotId(slot.getId())
                    || availabilityRepository.existsBySlotId(slot.getId())
                    || unidentifiedReportRepository.existsBySlotId(slot.getId())) {
                throw new BusinessException(SLOT_IN_USE);
            }
        }
        try {
            parkingSlotRepository.deleteAll(toDelete);
            parkingSlotRepository.flush(); // 방문 기록(FK)이 남아 있으면 여기서 위반 → 409
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(SLOT_IN_USE);
        }
        List<ParkingSlot> kept = new ArrayList<>(slots.subList(0, Math.min(slots.size(), newCount)));

        // 남은 면은 좌표만 다시 계산 (배정 세대는 유지), 늘린 면은 공용으로 추가
        List<SlotPosition> positions = SlotLayoutCalculator.calculate(request.layoutTemplate(), request.entranceSide(), newCount);
        for (int i = 0; i < newCount; i++) {
            if (i < kept.size()) {
                kept.get(i).relocate(positions.get(i));
            } else {
                kept.add(parkingSlotRepository.save(ParkingSlot.create(villa, null, labelOf(i), positions.get(i))));
            }
        }
        villa.changeLayout(request.layoutTemplate(), request.entranceSide());

        return new LayoutResponse(kept.stream().map(SlotLayoutResponse::from).toList());
    }

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

    // ===== 공통 =====

    // 0번째 면 → "1번"
    private String labelOf(int index) {
        return (index + 1) + "번";
    }

    // "3번" → 2 (범위 밖이거나 형식이 다르면 -1)
    private int labelIndex(String label, int slotCount) {
        for (int i = 0; i < slotCount; i++) {
            if (labelOf(i).equals(label.strip())) {
                return i;
            }
        }
        return -1;
    }
}
