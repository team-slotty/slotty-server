package com.slotty.server.domain.unidentified.service;

import com.slotty.server.domain.board.event.BoardChangedEvent;
import org.springframework.context.ApplicationEventPublisher;
import com.slotty.server.domain.parking.EnterMethod;
import com.slotty.server.domain.parking.ExitMethod;
import com.slotty.server.domain.parking.ParkingRecord;
import com.slotty.server.domain.parking.ParkingRecordRepository;
import com.slotty.server.domain.slot.ParkingSlot;
import com.slotty.server.domain.slot.ParkingSlotRepository;
import com.slotty.server.domain.unidentified.ReportStatus;
import com.slotty.server.domain.unidentified.ResolveType;
import com.slotty.server.domain.unidentified.UnidentifiedReport;
import com.slotty.server.domain.unidentified.UnidentifiedReportRepository;
import com.slotty.server.domain.unidentified.dto.*;
import com.slotty.server.domain.user.User;
import com.slotty.server.domain.user.UserRepository;
import com.slotty.server.domain.vehicle.Vehicle;
import com.slotty.server.domain.vehicle.VehicleRepository;
import com.slotty.server.domain.vehicle.dto.VehiclePlate;
import com.slotty.server.domain.villa.AdminChecker;
import com.slotty.server.domain.villa.Household;
import com.slotty.server.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static com.slotty.server.global.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UnidentifiedReportService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final Duration CLOCK_SKEW = Duration.ofMinutes(1); // 입차와 같게: 기기 시계가 조금 빠른 것은 허용
    private static final int KEEP_RESOLVED_DAYS = 7;                  // 해제 후 7일 지나면 이력에서 사라짐 (명세)
    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final ParkingSlotRepository parkingSlotRepository;
    private final ParkingRecordRepository parkingRecordRepository;
    private final VehicleRepository vehicleRepository;
    private final UnidentifiedReportRepository unidentifiedReportRepository;
    private final AdminChecker adminChecker;
    private final ApplicationEventPublisher eventPublisher;

    // ===== 43. 미확인 차량 기록 (관리자는 모든 면, 면 주인은 자기 면만. 공용 면은 관리자만) =====
    @Transactional
    public ReportResponse report(Long userId, ReportCreateRequest request) {
        User user = findResident(userId);
        ParkingSlot slot = parkingSlotRepository.findWithHouseholdByIdAndVillaId(request.slotId(), villaIdOf(user))
                .orElseThrow(() -> new BusinessException(SLOT_NOT_FOUND));
        if (!isAdmin(user) && !isSlotOwner(slot, user)) {
            throw new BusinessException(FORBIDDEN);
        }

        if (unidentifiedReportRepository.findBySlotIdAndResolvedAtIsNull(slot.getId()).isPresent()) {
            throw new BusinessException(REPORT_ALREADY_OPEN);
        }
        // 등록된 차량이 입차 기록을 남긴 면이면 미확인이 아님 (그 차의 연락처는 면 상세·검색으로 확인)
        if (parkingRecordRepository.findBySlotIdAndExitedAtIsNull(slot.getId()).isPresent()) {
            throw new BusinessException(SLOT_OCCUPIED);
        }

        String plateNumber = request.plateNumber() == null ? null : VehiclePlate.normalize(request.plateNumber());
        UnidentifiedReport report = UnidentifiedReport.report(slot, plateNumber, user, now());
        try {
            unidentifiedReportRepository.saveAndFlush(report);
        } catch (DataIntegrityViolationException e) { // 동시에 같은 면을 기록 (uk_report_open_slot)
            throw new BusinessException(REPORT_ALREADY_OPEN);
        }
        eventPublisher.publishEvent(BoardChangedEvent.of(villaIdOf(user), slot.getId()));
        return ReportResponse.from(report);
    }

    // ===== 44. [차량 떠남] 해제 (관리자·면 주인) =====
    @Transactional
    public ResolveResponse resolve(Long userId, Long reportId) {
        User user = findResident(userId);
        UnidentifiedReport report = findReport(reportId, user);
        if (!isAdmin(user) && !isSlotOwner(report.getSlot(), user)) {
            throw new BusinessException(FORBIDDEN);
        }
        if (!report.isOpen()) {
            throw new BusinessException(REPORT_ALREADY_RESOLVED); // 이미 떠남·내 차·정상 입차로 해제됨
        }
        report.resolve(user, ResolveType.LEFT, later(report.getReportedAt(), now()));
        eventPublisher.publishEvent(BoardChangedEvent.of(villaIdOf(user), report.getSlot().getId()));
        return ResolveResponse.from(report);
    }

    // ===== 45. [내 차예요] 정정 (면 주인만): 미확인을 MINE으로 해제 + CORRECTION 입차 기록 =====
    @Transactional
    public MineResponse correctMine(Long userId, Long reportId, MineRequest request) {
        User user = findResident(userId);
        LocalDateTime enteredAt = toKst(request.enteredAt());

        // 1. 같은 요청의 재전송이면 저장된 결과를 그대로 (입차와 같은 clientRequestId 규칙)
        Optional<ParkingRecord> sameRequest = parkingRecordRepository.findByClientRequestId(request.clientRequestId());
        if (sameRequest.isPresent()) {
            ParkingRecord record = sameRequest.get();
            return new MineResponse(reportId, record.getId(), ResolveType.MINE, record.getEnteredAt());
        }

        // 2. 기록·권한·상태 확인
        UnidentifiedReport report = findReport(reportId, user);
        ParkingSlot slot = report.getSlot();
        if (!isSlotOwner(slot, user)) {
            throw new BusinessException(FORBIDDEN); // 관리자라도 남의 면에 "내 차"라고 할 수는 없음
        }
        if (!report.isOpen()) {
            throw new BusinessException(REPORT_ALREADY_RESOLVED);
        }
        Vehicle vehicle = vehicleRepository.findByIdAndHouseholdId(request.vehicleId(), user.getHousehold().getId())
                .orElseThrow(() -> new BusinessException(VEHICLE_NOT_FOUND));

        // 3. 같은 차량의 다른 열린 기록은 출차 누락으로 보고 AUTO 정리 (입차 4단계와 같음)
        Long[] previousSlotId = {null}; // 람다 안에서 값을 바꾸려고 배열 한 칸
        parkingRecordRepository.findByVehicleIdAndExitedAtIsNull(vehicle.getId()).ifPresent(open -> {
            previousSlotId[0] = open.getSlot().getId();
            open.exit(null, ExitMethod.AUTO, later(open.getEnteredAt(), enteredAt));
            parkingRecordRepository.flush(); // INSERT보다 UPDATE를 먼저 (open_vehicle_id 유니크)
        });

        // 4. 면에 다른 차의 열린 기록이 있으면 거절
        if (parkingRecordRepository.findBySlotIdAndExitedAtIsNull(slot.getId()).isPresent()) {
            throw new BusinessException(SLOT_OCCUPIED);
        }

        // 5. CORRECTION 입차 기록 (동시 입차로 유니크 위반이면 409)
        ParkingRecord record = ParkingRecord.enter(slot, vehicle, user, EnterMethod.CORRECTION,
                request.clientRequestId(), enteredAt);
        try {
            parkingRecordRepository.saveAndFlush(record);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(SLOT_OCCUPIED);
        }

        // 6. 미확인 기록 MINE 해제 (ck_report_resolve: 해제 시각 >= 기록 시각)
        report.resolve(user, ResolveType.MINE, later(report.getReportedAt(), now()));
        // 이 면(미확인 → 사용 중) + 같은 차가 서 있던 다른 면(AUTO 정리)
        eventPublisher.publishEvent(BoardChangedEvent.of(villaIdOf(user), slot.getId(), previousSlotId[0]));
        return new MineResponse(report.getId(), record.getId(), ResolveType.MINE, record.getEnteredAt());
    }

    // ===== 46. 미확인 기록 이력 (관리자) =====
    public ReportHistoryResponse getHistory(Long userId, ReportStatus status, int page, int size) {
        User admin = adminChecker.requireAdmin(userId);
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        return ReportHistoryResponse.from(unidentifiedReportRepository.findHistoryByVillaId(
                villaIdOf(admin), status.toOpenFlag(), now().minusDays(KEEP_RESOLVED_DAYS), pageable));
    }

    // ===== 공통 =====

    private User findResident(Long userId) {
        return userRepository.findActiveWithHouseholdAndVilla(userId)
                .orElseThrow(() -> new BusinessException(NOT_IN_HOUSEHOLD));
    }

    // 다른 빌라 기록이면 404
    private UnidentifiedReport findReport(Long reportId, User user) {
        return unidentifiedReportRepository.findForUpdateByIdAndVillaId(reportId, villaIdOf(user))
                .orElseThrow(() -> new BusinessException(REPORT_NOT_FOUND));
    }

    private Long villaIdOf(User user) {
        return user.getHousehold().getVilla().getId();
    }

    private boolean isAdmin(User user) {
        User admin = user.getHousehold().getVilla().getAdminUser();
        return admin != null && admin.getId().equals(user.getId());
    }

    // 공용 면은 주인이 없으니 항상 false
    private boolean isSlotOwner(ParkingSlot slot, User user) {
        return slot.getHousehold() != null && slot.getHousehold().getId().equals(user.getHousehold().getId());
    }

    // 요청 시각(오프셋 포함) → KST LocalDateTime(초 단위). 생략하면 서버 시각, 미래면 400 (입차와 같은 규칙)
    private LocalDateTime toKst(OffsetDateTime requested) {
        LocalDateTime now = now();
        if (requested == null) {
            return now;
        }
        LocalDateTime kst = requested.atZoneSameInstant(KST).toLocalDateTime().truncatedTo(ChronoUnit.SECONDS);
        if (kst.isAfter(now.plus(CLOCK_SKEW))) {
            throw new BusinessException(VALIDATION_FAILED);
        }
        return kst;
    }

    private LocalDateTime later(LocalDateTime a, LocalDateTime b) {
        return a.isAfter(b) ? a : b;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(KST).truncatedTo(ChronoUnit.SECONDS);
    }
}
