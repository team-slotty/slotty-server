package com.slotty.server.domain.visit.service;

import com.slotty.server.domain.slot.ParkingSlot;
import com.slotty.server.domain.slot.ParkingSlotRepository;
import com.slotty.server.domain.user.User;
import com.slotty.server.domain.user.UserRepository;
import com.slotty.server.domain.vehicle.dto.VehiclePlate;
import com.slotty.server.domain.villa.Household;
import com.slotty.server.domain.visit.*;
import com.slotty.server.domain.visit.dto.*;
import com.slotty.server.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.slotty.server.global.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VisitService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final long MAX_VISIT_HOURS = 24;
    private static final int FREQUENT_LIMIT = 5;
    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final ParkingSlotRepository parkingSlotRepository;
    private final VisitRepository visitRepository;
    private final FrequentVehicleRepository frequentVehicleRepository;
    private final VisitSlotPolicy visitSlotPolicy;

    // ===== 35. 방문차량 등록 =====
    // READ_COMMITTED: 면 락을 기다렸다 받은 뒤의 겹침 조회가 "방금 커밋된 다른 방문"을 보도록
    // (MySQL 기본 REPEATABLE_READ는 트랜잭션 첫 조회 시점의 스냅샷을 계속 읽어서, 락을 기다려도 앞 요청의 방문이 안 보임)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public VisitResponse register(Long userId, VisitCreateRequest request) {
        User user = findResident(userId);
        Household household = user.getHousehold();
        LocalDateTime now = now();

        LocalDateTime startAt = toKst(request.startAt());
        LocalDateTime endAt = toKst(request.endAt());
        validatePeriod(startAt, endAt, now);
        String plateNumber = VehiclePlate.normalize(request.plateNumber());

        ParkingSlot slot = request.slotId() == null ? null
                : lockAssignableSlot(request.slotId(), household, startAt, endAt, null);

        Visit visit = visitRepository.save(Visit.register(household, user, plateNumber, startAt, endAt, slot));
        rememberFrequentVehicle(household, plateNumber, now);
        return VisitResponse.of(visit, now);
    }

    // ===== 36. 방문 지정 가능한 면 =====
    public List<AvailableSlotResponse> getAvailableSlots(Long userId, OffsetDateTime startAt, OffsetDateTime endAt) {
        User user = findResident(userId);
        Long villaId = user.getHousehold().getVilla().getId();
        LocalDateTime start = toKst(startAt);
        LocalDateTime end = toKst(endAt);
        validatePeriod(start, end, now());

        // 그 시간에 다른 방문이 이미 지정한 면은 한 번에 모아 제외
        Set<Long> occupied = new HashSet<>(visitRepository.findOccupiedSlotIds(villaId, start, end));

        return parkingSlotRepository.findAllWithHouseholdByVillaId(villaId).stream()
                .filter(slot -> !occupied.contains(slot.getId()))
                .flatMap(slot -> visitSlotPolicy.findWindow(slot, start, end)
                        .map(window -> AvailableSlotResponse.of(slot, window))
                        .stream())
                .toList();
    }

    // ===== 37. 우리 세대 방문 목록 =====
    public VisitPageResponse getVisits(Long userId, VisitScope scope, int page, int size) {
        User user = findResident(userId);
        Long householdId = user.getHousehold().getId();
        LocalDateTime now = now();
        // 잘못된 값은 에러 대신 가까운 정상 값으로 (이력 API와 같은 규칙)
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));

        Page<Visit> visits = switch (scope) {
            case UPCOMING -> visitRepository.findUpcoming(householdId, now, pageable);
            case ONGOING -> visitRepository.findOngoing(householdId, now, pageable);
            case PAST -> visitRepository.findPast(householdId, now, pageable);
        };
        return VisitPageResponse.of(visits, now);
    }

    // ===== 38. 방문 상세 (방문 세대·면 주인·관리자, 그 외는 404) =====
    public VisitDetailResponse getVisit(Long userId, Long visitId) {
        User user = findResident(userId);
        Visit visit = visitRepository.findWithDetailsById(visitId)
                .filter(v -> canView(v, user))
                .orElseThrow(() -> new BusinessException(VISIT_NOT_FOUND));
        return VisitDetailResponse.of(visit, now());
    }

    // ===== 39. 방문 수정 (방문 세대, 시작 전만) =====
    @Transactional(isolation = Isolation.READ_COMMITTED) // 등록과 같은 이유
    public VisitResponse update(Long userId, Long visitId, VisitUpdateRequest request) {
        User user = findResident(userId);
        Visit visit = findMyVisit(visitId, user);
        LocalDateTime now = now();
        if (visit.isStarted(now)) {
            throw new BusinessException(VISIT_ALREADY_STARTED);
        }

        // 보낸 필드만 반영한 "수정 후" 값
        LocalDateTime startAt = request.getStartAt() == null ? visit.getStartAt() : toKst(request.getStartAt());
        LocalDateTime endAt = request.getEndAt() == null ? visit.getEndAt() : toKst(request.getEndAt());
        Long currentSlotId = visit.getSlot() == null ? null : visit.getSlot().getId();
        Long slotId = request.isSlotIdPresent() ? request.getSlotId() : currentSlotId;
        validatePeriod(startAt, endAt, now);

        boolean periodChanged = !startAt.equals(visit.getStartAt()) || !endAt.equals(visit.getEndAt());
        boolean slotChanged = !Objects.equals(slotId, currentSlotId);

        // 면이 바뀌었거나 시간이 바뀌었으면 면 지정 조건을 다시 검사 (자기 자신과의 겹침은 제외)
        if (slotId != null && (slotChanged || periodChanged)) {
            ParkingSlot slot = lockAssignableSlot(slotId, user.getHousehold(), startAt, endAt, visit.getId());
            visit.changeSlot(slot); // 다시 검사를 통과했으니 이전 취소 표시도 지움
        } else if (slotId == null && currentSlotId != null) {
            visit.changeSlot(null); // 면 지정 해제
        }

        visit.changePeriod(startAt, endAt);
        if (request.getPlateNumber() != null) {
            String plateNumber = VehiclePlate.normalize(request.getPlateNumber());
            visit.changePlateNumber(plateNumber);
            rememberFrequentVehicle(user.getHousehold(), plateNumber, now);
        }
        return VisitResponse.of(visit, now);
    }

    // ===== 40. 방문 삭제 (방문 세대, 시작 전만) =====
    @Transactional
    public void delete(Long userId, Long visitId) {
        User user = findResident(userId);
        Visit visit = findMyVisit(visitId, user);
        if (visit.isStarted(now())) {
            throw new BusinessException(VISIT_ALREADY_STARTED); // 시작 후에는 41번 [방문 종료]
        }
        visitRepository.delete(visit);
    }

    // ===== 41. [방문 종료] (방문 세대) =====
    @Transactional
    public VisitResponse end(Long userId, Long visitId) {
        User user = findResident(userId);
        Visit visit = findMyVisit(visitId, user);
        LocalDateTime now = now();

        VisitState state = visit.stateAt(now);
        if (state == VisitState.SCHEDULED) {
            throw new BusinessException(VISIT_NOT_STARTED); // 시작 전은 40번 삭제
        }
        if (state == VisitState.ENDED) {
            throw new BusinessException(VISIT_ALREADY_ENDED); // 이미 눌렀거나, 면 없는 방문이 종료 시각에 자동 종료됨
        }
        visit.end(user, now);
        return VisitResponse.of(visit, now);
    }

    // ===== 42. 자주 오는 방문차 =====
    public List<FrequentVehicleResponse> getFrequentVehicles(Long userId) {
        User user = findResident(userId);
        return frequentVehicleRepository.findTop5ByHouseholdIdOrderByLastVisitedAtDescIdDesc(user.getHousehold().getId())
                .stream()
                .map(FrequentVehicleResponse::from)
                .toList();
    }

    // ===== 공통 =====

    private User findResident(Long userId) {
        return userRepository.findActiveWithHouseholdAndVilla(userId)
                .orElseThrow(() -> new BusinessException(NOT_IN_HOUSEHOLD));
    }

    // 종료 > 시작, 24시간 이하, 종료가 미래 (DB CHECK ck_visit_period가 막기 전에 400으로)
    private void validatePeriod(LocalDateTime startAt, LocalDateTime endAt, LocalDateTime now) {
        if (!endAt.isAfter(startAt) || endAt.isAfter(startAt.plusHours(MAX_VISIT_HOURS)) || !endAt.isAfter(now)) {
            throw new BusinessException(VISIT_PERIOD_INVALID);
        }
    }

    // 면 지정 검사: 우리 빌라 면(404) → 공용 또는 가용시간이 덮는 면(409 SLOT_NOT_AVAILABLE) → 다른 방문과 겹침(409 VISIT_OVERLAP)
    // 면 행에 비관적 락(SELECT ... FOR UPDATE)을 걸어, 같은 면을 동시에 지정하는 요청은 한 줄로 세움
    // (명세의 Redis 분산 락 대신: 서버가 1대이고 DB 트랜잭션 안에서 끝나므로 락도 트랜잭션과 같이 풀리는 DB 락이 단순함)
    private ParkingSlot lockAssignableSlot(Long slotId, Household household,
                                           LocalDateTime startAt, LocalDateTime endAt, Long excludeVisitId) {
        ParkingSlot slot = parkingSlotRepository.findForUpdateByIdAndVillaId(slotId, household.getVilla().getId())
                .orElseThrow(() -> new BusinessException(SLOT_NOT_FOUND));
        if (!visitSlotPolicy.covers(slot, startAt, endAt)) {
            throw new BusinessException(SLOT_NOT_AVAILABLE);
        }
        if (visitRepository.countOverlapping(slot.getId(), startAt, endAt, excludeVisitId) > 0) {
            throw new BusinessException(VISIT_OVERLAP);
        }
        return slot;
    }

    // 자주 오는 차 갱신: 있으면 시각만, 없으면 추가 → 세대당 최근 5대만 남김
    private void rememberFrequentVehicle(Household household, String plateNumber, LocalDateTime now) {
        Optional<FrequentVehicle> existing = frequentVehicleRepository.findByHouseholdIdAndPlateNumber(household.getId(), plateNumber);
        if (existing.isPresent()) {
            existing.get().touch(now);
        } else {
            frequentVehicleRepository.save(FrequentVehicle.create(household, plateNumber, now));
        }
        frequentVehicleRepository.flush(); // 아래 조회에 방금 바꾼 시각이 반영되도록

        List<FrequentVehicle> all = frequentVehicleRepository.findByHouseholdIdOrderByLastVisitedAtDescIdDesc(household.getId());
        if (all.size() > FREQUENT_LIMIT) {
            frequentVehicleRepository.deleteAll(all.subList(FREQUENT_LIMIT, all.size()));
        }
    }

    // 수정·삭제·종료는 방문 세대만 (다른 세대 방문은 존재를 숨기려고 404)
    private Visit findMyVisit(Long visitId, User user) {
        return visitRepository.findWithDetailsById(visitId)
                .filter(v -> v.getHousehold().getId().equals(user.getHousehold().getId()))
                .orElseThrow(() -> new BusinessException(VISIT_NOT_FOUND));
    }

    // 상세는 방문 세대 + 지정 면 주인 + 같은 빌라 관리자
    private boolean canView(Visit visit, User user) {
        Household mine = user.getHousehold();
        if (!visit.getHousehold().getVilla().getId().equals(mine.getVilla().getId())) {
            return false; // 다른 빌라
        }
        boolean visitingHousehold = visit.getHousehold().getId().equals(mine.getId());
        boolean slotOwner = visit.getSlot() != null && visit.getSlot().getHousehold() != null
                && visit.getSlot().getHousehold().getId().equals(mine.getId());
        User admin = mine.getVilla().getAdminUser();
        boolean isAdmin = admin != null && admin.getId().equals(user.getId());
        return visitingHousehold || slotOwner || isAdmin;
    }

    // 요청의 +09:00 시각을 KST LocalDateTime으로 (초 아래는 버림)
    private LocalDateTime toKst(OffsetDateTime time) {
        return time.atZoneSameInstant(KST).toLocalDateTime().truncatedTo(ChronoUnit.SECONDS);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(KST).truncatedTo(ChronoUnit.SECONDS);
    }
}
