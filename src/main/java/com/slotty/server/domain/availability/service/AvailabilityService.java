package com.slotty.server.domain.availability.service;

import com.slotty.server.domain.board.event.BoardChangedEvent;
import org.springframework.context.ApplicationEventPublisher;
import com.slotty.server.domain.availability.Availability;
import com.slotty.server.domain.availability.AvailabilityException;
import com.slotty.server.domain.availability.AvailabilityExceptionRepository;
import com.slotty.server.domain.availability.AvailabilityRepository;
import com.slotty.server.domain.availability.Weekday;
import com.slotty.server.domain.availability.dto.AvailabilityRequest;
import com.slotty.server.domain.availability.dto.AvailabilityResponse;
import com.slotty.server.domain.availability.dto.SkipRequest;
import com.slotty.server.domain.availability.dto.SkipResponse;
import com.slotty.server.domain.slot.ParkingSlot;
import com.slotty.server.domain.slot.ParkingSlotRepository;
import com.slotty.server.domain.user.User;
import com.slotty.server.domain.user.UserRepository;
import com.slotty.server.domain.villa.Household;
import com.slotty.server.domain.visit.VisitSlotPolicy;
import com.slotty.server.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.slotty.server.global.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AvailabilityService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final UserRepository userRepository;
    private final ParkingSlotRepository parkingSlotRepository;
    private final AvailabilityRepository availabilityRepository;
    private final AvailabilityExceptionRepository availabilityExceptionRepository;
    private final VisitSlotPolicy visitSlotPolicy;
    private final ApplicationEventPublisher eventPublisher;

    // ===== 47. 우리 면 가용시간 목록 =====
    public List<AvailabilityResponse> getAvailabilities(Long userId, Long slotId) {
        User user = findResident(userId);
        Household household = user.getHousehold();
        requireAssignedSlot(household);
        if (slotId != null) {
            findMySlot(slotId, household); // 남의 면이면 403·404
        }

        List<Availability> availabilities = availabilityRepository.findWithSlotByHouseholdId(household.getId(), slotId);

        // 해제 날짜를 가용시간마다 따로 조회하지 않고 한 번에 가져와 Map으로 묶음 (N+1 방지)
        LocalDate today = today();
        Map<Long, List<LocalDate>> skippedById = availabilities.isEmpty() ? Map.of()
                : availabilityExceptionRepository
                .findByAvailabilityIdInAndSkipDateGreaterThanEqualOrderBySkipDate(
                        availabilities.stream().map(Availability::getId).toList(), today)
                .stream()
                .collect(Collectors.groupingBy(e -> e.getAvailability().getId(),
                        Collectors.mapping(AvailabilityException::getSkipDate, Collectors.toList())));

        return availabilities.stream()
                .map(a -> AvailabilityResponse.of(a, skippedById.getOrDefault(a.getId(), List.of())))
                .toList();
    }

    // ===== 48. 가용시간 등록 =====
    @Transactional
    public AvailabilityResponse register(Long userId, AvailabilityRequest request) {
        User user = findResident(userId);
        Household household = user.getHousehold();
        requireAssignedSlot(household);
        ParkingSlot slot = findMySlot(request.slotId(), household);

        validate(request);
        Availability availability = request.isRecurring()
                ? Availability.recurring(slot, user, Weekday.toMask(request.days()),
                request.allDay(), request.startTime(), request.endTime())
                : Availability.once(slot, user, request.targetDate(),
                request.allDay(), request.startTime(), request.endTime());

        availabilityRepository.save(availability);
        eventPublisher.publishEvent(BoardChangedEvent.of(household.getVilla().getId(), slot.getId()));
        return AvailabilityResponse.of(availability, List.of());
    }

    // ===== 49. 가용시간 삭제 =====
    @Transactional
    public void delete(Long userId, Long availabilityId) {
        User user = findResident(userId);
        Availability availability = findMyAvailability(availabilityId, user.getHousehold());

        // FK(availability_exception.availability_id) 때문에 해제 예외를 먼저 삭제
        availabilityExceptionRepository.deleteAllByAvailabilityId(availability.getId());
        availabilityRepository.delete(availability);
        availabilityRepository.flush(); // 아래 재검사가 삭제된 가용시간을 빼고 보도록 DB에 먼저 반영

        // 이 가용시간이 사라져 시간이 더 이상 덮이지 않는 방문은 면 지정 취소 표시 (명세 49 부수 효과)
        visitSlotPolicy.cancelBrokenVisits(availability.getSlot(), LocalDateTime.now(KST));
        eventPublisher.publishEvent(BoardChangedEvent.of(user.getHousehold().getVilla().getId(), availability.getSlot().getId()));
    }

    // ===== 50. [이번만 해제] =====
    @Transactional
    public SkipResponse skip(Long userId, Long availabilityId, SkipRequest request) {
        User user = findResident(userId);
        Availability availability = findMyAvailability(availabilityId, user.getHousehold());
        LocalDate skipDate = request.skipDate();

        if (skipDate.isBefore(today()) || !availability.isScheduledOn(skipDate)) {
            throw new BusinessException(AVAILABILITY_INVALID); // 지난 날짜, 또는 원래 회차가 없는 날
        }
        if (availabilityExceptionRepository.existsByAvailabilityIdAndSkipDate(availability.getId(), skipDate)) {
            throw new BusinessException(EXCEPTION_ALREADY_EXISTS);
        }
        try {
            availabilityExceptionRepository.saveAndFlush(AvailabilityException.skip(availability, skipDate, user));
        } catch (DataIntegrityViolationException e) { // 동시에 같은 날짜를 해제 (uk_exception_availability_date)
            throw new BusinessException(EXCEPTION_ALREADY_EXISTS);
        }
        // 그 날짜에 이 가용시간에 기대던 방문은 면 지정 취소 표시 (명세 50 부수 효과)
        visitSlotPolicy.cancelBrokenVisits(availability.getSlot(), LocalDateTime.now(KST));
        eventPublisher.publishEvent(BoardChangedEvent.of(user.getHousehold().getVilla().getId(), availability.getSlot().getId()));
        return new SkipResponse(availability.getId(), skipDate);
    }

    // ===== 공통 =====

    private User findResident(Long userId) {
        return userRepository.findActiveWithHouseholdAndVilla(userId)
                .orElseThrow(() -> new BusinessException(NOT_IN_HOUSEHOLD));
    }

    // 배정 면이 없는 세대는 가용시간 기능 자체를 쓸 수 없음 (앱도 메뉴를 숨김)
    private void requireAssignedSlot(Household household) {
        if (!parkingSlotRepository.existsByHouseholdId(household.getId())) {
            throw new BusinessException(NO_ASSIGNED_SLOT);
        }
    }

    // 우리 빌라 면이 아니면 404, 우리 빌라지만 내 세대 면이 아니면 403
    private ParkingSlot findMySlot(Long slotId, Household household) {
        ParkingSlot slot = parkingSlotRepository.findWithHouseholdByIdAndVillaId(slotId, household.getVilla().getId())
                .orElseThrow(() -> new BusinessException(SLOT_NOT_FOUND));
        if (slot.getHousehold() == null || !slot.getHousehold().getId().equals(household.getId())) {
            throw new BusinessException(NOT_SLOT_OWNER);
        }
        return slot;
    }

    // 다른 빌라 가용시간이면 404, 우리 빌라 다른 세대 면이면 403
    private Availability findMyAvailability(Long availabilityId, Household household) {
        Availability availability = availabilityRepository.findWithSlotById(availabilityId)
                .filter(a -> a.getSlot().getVilla().getId().equals(household.getVilla().getId()))
                .orElseThrow(() -> new BusinessException(AVAILABILITY_NOT_FOUND));
        ParkingSlot slot = availability.getSlot();
        if (slot.getHousehold() == null || !slot.getHousehold().getId().equals(household.getId())) {
            throw new BusinessException(NOT_SLOT_OWNER);
        }
        return availability;
    }

    // DB CHECK(ck_availability_repeat, ck_availability_time)가 막기 전에 400으로
    private void validate(AvailabilityRequest request) {
        boolean repeatOk = request.isRecurring()
                ? request.days() != null && !request.days().isEmpty() && request.targetDate() == null
                : (request.days() == null || request.days().isEmpty())
                && request.targetDate() != null && !request.targetDate().isBefore(today());

        boolean timeOk = request.allDay()
                ? request.startTime() == null && request.endTime() == null
                : isHalfHour(request.startTime()) && isHalfHour(request.endTime())
                && request.startTime().isBefore(request.endTime());

        if (!repeatOk || !timeOk) {
            throw new BusinessException(AVAILABILITY_INVALID);
        }
    }

    // 30분 단위 (09:00, 09:30 O / 09:15 X)
    private boolean isHalfHour(LocalTime time) {
        return time != null && time.getMinute() % 30 == 0 && time.getSecond() == 0;
    }

    private LocalDate today() {
        return LocalDate.now(KST);
    }
}
