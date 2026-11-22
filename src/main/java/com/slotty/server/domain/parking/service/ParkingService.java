package com.slotty.server.domain.parking.service;

import com.slotty.server.domain.availability.Availability;
import com.slotty.server.domain.availability.AvailabilityException;
import com.slotty.server.domain.availability.AvailabilityExceptionRepository;
import com.slotty.server.domain.availability.AvailabilityRepository;
import com.slotty.server.domain.board.VehicleState;
import com.slotty.server.domain.parking.CheckInMode;
import com.slotty.server.domain.parking.EnterMethod;
import com.slotty.server.domain.parking.ExitMethod;
import com.slotty.server.domain.parking.ParkingRecord;
import com.slotty.server.domain.parking.ParkingRecordRepository;
import com.slotty.server.domain.parking.SlotType;
import com.slotty.server.domain.parking.dto.CheckInOptionsResponse;
import com.slotty.server.domain.parking.dto.CheckInOptionsResponse.SlotOption;
import com.slotty.server.domain.parking.dto.CheckInOptionsResponse.SlotOptions;
import com.slotty.server.domain.parking.dto.CheckInOptionsResponse.VehicleOption;
import com.slotty.server.domain.parking.dto.CheckInRequest;
import com.slotty.server.domain.parking.dto.CheckInResponse;
import com.slotty.server.domain.parking.dto.CheckInResult;
import com.slotty.server.domain.slot.ParkingSlot;
import com.slotty.server.domain.slot.ParkingSlotRepository;
import com.slotty.server.domain.unidentified.UnidentifiedReportRepository;
import com.slotty.server.domain.user.User;
import com.slotty.server.domain.user.UserRepository;
import com.slotty.server.domain.vehicle.Vehicle;
import com.slotty.server.domain.vehicle.VehicleRepository;
import com.slotty.server.domain.villa.Household;
import com.slotty.server.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.slotty.server.domain.parking.CheckOutMode;
import com.slotty.server.domain.parking.dto.CheckOutOptionsResponse;
import com.slotty.server.domain.parking.dto.CheckOutOptionsResponse.ParkedVehicle;
import com.slotty.server.domain.parking.dto.CheckOutRequest;
import com.slotty.server.domain.parking.dto.CheckOutResponse;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.slotty.server.global.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParkingService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final Duration PASSENGER_WINDOW = Duration.ofMinutes(5); // 동승자 규칙
    private static final Duration CLOCK_SKEW = Duration.ofMinutes(1);       // 기기 시계가 조금 빠른 것은 허용

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final ParkingSlotRepository parkingSlotRepository;
    private final ParkingRecordRepository parkingRecordRepository;
    private final UnidentifiedReportRepository unidentifiedReportRepository;
    private final AvailabilityRepository availabilityRepository;
    private final AvailabilityExceptionRepository availabilityExceptionRepository;

    // ===== 30. 입차 옵션 =====
    public CheckInOptionsResponse getCheckInOptions(Long userId) {
        User user = findResident(userId);
        Household household = user.getHousehold();
        Long villaId = household.getVilla().getId();

        List<Vehicle> vehicles = vehicleRepository.findByHouseholdIdOrderById(household.getId());
        if (vehicles.isEmpty()) {
            throw new BusinessException(NO_VEHICLE);
        }

        List<ParkingSlot> slots = parkingSlotRepository.findAllWithHouseholdByVillaId(villaId);
        List<ParkingRecord> openRecords = parkingRecordRepository.findOpenWithVehicleByVillaId(villaId);

        Map<Long, String> labelBySlotId = slots.stream()
                .collect(Collectors.toMap(ParkingSlot::getId, ParkingSlot::getLabel));
        Map<Long, Long> slotIdByVehicleId = openRecords.stream()
                .collect(Collectors.toMap(r -> r.getVehicle().getId(), r -> r.getSlot().getId()));
        Set<Long> occupiedSlotIds = openRecords.stream()
                .map(r -> r.getSlot().getId())
                .collect(Collectors.toSet());

        // 차량: 열린 기록이 없으면 운행 중(DRIVING)
        List<VehicleOption> allVehicles = vehicles.stream()
                .map(v -> {
                    Long parkedSlotId = slotIdByVehicleId.get(v.getId());
                    return new VehicleOption(
                            v.getId(),
                            v.getPlateNumber(),
                            parkedSlotId == null ? VehicleState.DRIVING : VehicleState.PARKED,
                            parkedSlotId == null ? null : labelBySlotId.get(parkedSlotId)
                    );
                })
                .toList();
        List<VehicleOption> drivingVehicles = allVehicles.stream()
                .filter(v -> v.state() == VehicleState.DRIVING)
                .toList();

        CheckInMode mode = switch (drivingVehicles.size()) {
            case 0 -> CheckInMode.CORRECTION;
            case 1 -> CheckInMode.AUTO;
            default -> CheckInMode.SELECT;
        };
        List<VehicleOption> vehicleOptions = (mode == CheckInMode.CORRECTION) ? allVehicles : drivingVehicles;
        Long suggestedVehicleId = (mode == CheckInMode.AUTO) ? drivingVehicles.get(0).vehicleId() : null;

        // 면: 빈 배정 면 → 빈 공용 면
        List<SlotOption> assignedOptions = new ArrayList<>();
        List<SlotOption> commonOptions = new ArrayList<>();
        boolean hasAssignedSlot = false;
        for (ParkingSlot slot : slots) {
            boolean common = slot.getHousehold() == null;
            boolean mine = !common && slot.getHousehold().getId().equals(household.getId());
            if (mine) {
                hasAssignedSlot = true;
            }
            if (occupiedSlotIds.contains(slot.getId())) {
                continue;
            }
            if (mine) {
                assignedOptions.add(new SlotOption(slot.getId(), slot.getLabel(), SlotType.ASSIGNED));
            } else if (common) {
                commonOptions.add(new SlotOption(slot.getId(), slot.getLabel(), SlotType.COMMON));
            }
        }
        Long suggestedSlotId = assignedOptions.isEmpty() ? null : assignedOptions.get(0).slotId();
        boolean assignedAllOccupied = hasAssignedSlot && assignedOptions.isEmpty();

        List<SlotOption> slotOptions = new ArrayList<>(assignedOptions);
        slotOptions.addAll(commonOptions);

        return new CheckInOptionsResponse(
                mode,
                vehicleOptions,
                suggestedVehicleId,
                new SlotOptions(suggestedSlotId, assignedAllOccupied, slotOptions)
        );
    }

    // ===== 31. 입차 =====
    @Transactional
    public CheckInResult checkIn(Long userId, CheckInRequest request) {
        if (request.method() == EnterMethod.CORRECTION) { // CORRECTION은 [내 차예요] 전용
            throw new BusinessException(VALIDATION_FAILED);
        }
        LocalDateTime enteredAt = toKst(request.enteredAt());

        // 1. 같은 요청의 재전송이면 저장된 결과를 그대로
        Optional<ParkingRecord> sameRequest = parkingRecordRepository.findByClientRequestId(request.clientRequestId());
        if (sameRequest.isPresent()) {
            return new CheckInResult(false, CheckInResponse.existing(sameRequest.get(), false));
        }

        // 2. 내 세대 차량인지, 우리 빌라의 내 배정 면 또는 공용 면인지
        User user = findResident(userId);
        Household household = user.getHousehold();

        Vehicle vehicle = vehicleRepository.findByIdAndHouseholdId(request.vehicleId(), household.getId())
                .orElseThrow(() -> new BusinessException(VEHICLE_NOT_FOUND));
        ParkingSlot slot = parkingSlotRepository.findByIdAndVillaId(request.slotId(), household.getVilla().getId())
                .orElseThrow(() -> new BusinessException(SLOT_NOT_FOUND));

        boolean myAssignedSlot = slot.getHousehold() != null
                && slot.getHousehold().getId().equals(household.getId());
        if (slot.getHousehold() != null && !myAssignedSlot) {
            throw new BusinessException(SLOT_NOT_ALLOWED);
        }

        // 3·4. 같은 차량의 열린 기록
        Long autoClosedRecordId = null;
        Optional<ParkingRecord> openOfVehicle = parkingRecordRepository.findByVehicleIdAndExitedAtIsNull(vehicle.getId());
        if (openOfVehicle.isPresent()) {
            ParkingRecord open = openOfVehicle.get();

            // 3. 동승자: 다른 사용자가 5분 안에 같은 차를 입차시켰으면 무시
            if (isPassenger(open, user, enteredAt)) {
                return new CheckInResult(false, CheckInResponse.existing(open, true));
            }

            // 4. 출차 누락 정리 (exited_at >= entered_at CHECK 때문에 더 늦은 시각으로)
            open.exit(null, ExitMethod.AUTO, later(open.getEnteredAt(), enteredAt));
            autoClosedRecordId = open.getId();
            parkingRecordRepository.flush(); // INSERT보다 UPDATE를 먼저 DB에 보냄
        }

        // 5. 면에 다른 차의 열린 기록이 있으면 거절
        if (parkingRecordRepository.findBySlotIdAndExitedAtIsNull(slot.getId()).isPresent()) {
            throw new BusinessException(SLOT_OCCUPIED);
        }

        // 6. 기록 생성 (동시 입차로 유니크 제약을 위반하면 409)
        ParkingRecord record = ParkingRecord.enter(
                slot, vehicle, user, request.method(), request.clientRequestId(), enteredAt);
        try {
            parkingRecordRepository.saveAndFlush(record);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(SLOT_OCCUPIED);
        }

        // 7. 이 면의 미확인 기록 자동 해제
        boolean unidentifiedResolved = unidentifiedReportRepository.findBySlotIdAndResolvedAtIsNull(slot.getId())
                .map(report -> {
                    report.resolveAuto(later(report.getReportedAt(), LocalDateTime.now(KST)));
                    return true;
                })
                .orElse(false);

        // 8. 내 배정 면이고 지금이 가용시간 회차면 오늘 날짜 예외 생성 (주인 입차 우선)
        boolean availabilitySkipped = myAssignedSlot && skipOpenAvailabilities(slot, user, enteredAt);

        return new CheckInResult(true, CheckInResponse.of(
                record, false, autoClosedRecordId, unidentifiedResolved, availabilitySkipped));
    }

    // ===== 32. 출차 옵션 =====
    public CheckOutOptionsResponse getCheckOutOptions(Long userId) {
        User user = findResident(userId);

        List<ParkedVehicle> parked = parkingRecordRepository
                .findOpenWithVehicleAndSlotByHouseholdId(user.getHousehold().getId())
                .stream()
                .map(ParkedVehicle::from)
                .toList();
        if (parked.isEmpty()) {
            throw new BusinessException(NOT_PARKED);
        }

        CheckOutMode mode = (parked.size() == 1) ? CheckOutMode.AUTO : CheckOutMode.SELECT;
        Long suggestedVehicleId = (mode == CheckOutMode.AUTO) ? parked.get(0).vehicleId() : null;

        return new CheckOutOptionsResponse(mode, parked, suggestedVehicleId);
    }

    // ===== 33. 출차 =====
    @Transactional
    public CheckOutResponse checkOut(Long userId, CheckOutRequest request) {
        if (request.method() == ExitMethod.AUTO) { // AUTO는 서버가 출차 누락을 정리할 때만 씀
            throw new BusinessException(VALIDATION_FAILED);
        }
        LocalDateTime exitedAt = toKst(request.exitedAt());

        User user = findResident(userId);
        Vehicle vehicle = vehicleRepository.findByIdAndHouseholdId(request.vehicleId(), user.getHousehold().getId())
                .orElseThrow(() -> new BusinessException(VEHICLE_NOT_FOUND));

        // 열린 기록이 없으면 409 (이미 출차한 재전송 포함 → 앱은 성공으로 간주)
        ParkingRecord open = parkingRecordRepository.findByVehicleIdAndExitedAtIsNull(vehicle.getId())
                .orElseThrow(() -> new BusinessException(NOT_PARKED));

        // 출차 시각이 입차 시각보다 앞서면 400 (DB CHECK exited_at >= entered_at 보다 먼저 막음)
        if (exitedAt.isBefore(open.getEnteredAt())) {
            throw new BusinessException(VALIDATION_FAILED);
        }

        open.exit(user, request.method(), exitedAt); // dirty checking으로 UPDATE
        return CheckOutResponse.from(open);
    }


    // ===== 공통 =====

    // 세대에 소속된 활성 사용자 (아니면 403)
    private User findResident(Long userId) {
        return userRepository.findActiveWithHouseholdAndVilla(userId)
                .orElseThrow(() -> new BusinessException(NOT_IN_HOUSEHOLD));
    }

    // 요청 시각(오프셋 포함) → KST LocalDateTime(초 단위). 생략하면 서버 시각, 미래면 400
    private LocalDateTime toKst(OffsetDateTime requested) {
        LocalDateTime now = LocalDateTime.now(KST).truncatedTo(ChronoUnit.SECONDS); // DB DATETIME은 초 단위
        if (requested == null) {
            return now;
        }
        LocalDateTime kst = requested.atZoneSameInstant(KST).toLocalDateTime().truncatedTo(ChronoUnit.SECONDS);
        if (kst.isAfter(now.plus(CLOCK_SKEW))) {
            throw new BusinessException(VALIDATION_FAILED);
        }
        return kst;
    }

    private boolean isPassenger(ParkingRecord open, User me, LocalDateTime enteredAt) {
        boolean enteredByOther = !open.getEnteredBy().getId().equals(me.getId());
        boolean withinWindow = Duration.between(open.getEnteredAt(), enteredAt).abs().compareTo(PASSENGER_WINDOW) <= 0;
        return enteredByOther && withinWindow;
    }

    private boolean skipOpenAvailabilities(ParkingSlot slot, User user, LocalDateTime at) {
        LocalDate today = at.toLocalDate();
        boolean skipped = false;
        for (Availability availability : availabilityRepository.findBySlotId(slot.getId())) {
            if (availability.isOpenAt(at)
                    && !availabilityExceptionRepository.existsByAvailabilityIdAndSkipDate(availability.getId(), today)) {
                availabilityExceptionRepository.save(AvailabilityException.skip(availability, today, user));
                skipped = true;
            }
        }
        return skipped;
    }

    private LocalDateTime later(LocalDateTime a, LocalDateTime b) {
        return a.isAfter(b) ? a : b;
    }
}
