package com.slotty.server.domain.board.service;

import com.slotty.server.domain.availability.Availability;
import com.slotty.server.domain.availability.AvailabilityChecker;
import com.slotty.server.domain.board.SlotStatus;
import com.slotty.server.domain.board.SlotStatusResolver;
import com.slotty.server.domain.board.VehicleState;
import com.slotty.server.domain.board.dto.*;
import com.slotty.server.domain.parking.ParkingRecord;
import com.slotty.server.domain.parking.ParkingRecordRepository;
import com.slotty.server.domain.slot.ParkingSlot;
import com.slotty.server.domain.slot.ParkingSlotRepository;
import com.slotty.server.domain.unidentified.UnidentifiedReport;
import com.slotty.server.domain.unidentified.UnidentifiedReportRepository;
import com.slotty.server.domain.user.User;
import com.slotty.server.domain.user.UserRepository;
import com.slotty.server.domain.vehicle.Vehicle;
import com.slotty.server.domain.vehicle.VehicleRepository;
import com.slotty.server.domain.villa.Household;
import com.slotty.server.domain.villa.Villa;
import com.slotty.server.domain.visit.Visit;
import com.slotty.server.domain.visit.VisitRepository;
import com.slotty.server.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.slotty.server.global.exception.ErrorCode.NOT_IN_HOUSEHOLD;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 조회만 함
public class BoardService {
    private final UserRepository userRepository;
    private final ParkingSlotRepository parkingSlotRepository;
    private final ParkingRecordRepository parkingRecordRepository;
    private final UnidentifiedReportRepository unidentifiedReportRepository;
    private final VisitRepository visitRepository;
    private final VehicleRepository vehicleRepository;
    private final AvailabilityChecker availabilityChecker;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    public BoardResponse getBoard(Long userId) {

        // 1. 사용자 + 세대 + 빌라를 한 번에 조회, 없으면 403
        User user = userRepository.findActiveWithHouseholdAndVilla(userId)
                .orElseThrow(() -> new BusinessException(NOT_IN_HOUSEHOLD)); // 403

        // 2. 빌라 꺼내기
        Villa villa = user.getHousehold().getVilla();

        // 3. 이 빌라의 면 목록
        List<ParkingSlot> slots = parkingSlotRepository
                .findAllWithHouseholdByVillaId(villa.getId());

        // 3-1. 지금 서 있는 차 (출차 안 한 기록)
        List<ParkingRecord> openRecords = parkingRecordRepository
                .findOpenWithVehicleByVillaId(villa.getId());

        // 3-2. 면 id로 기록을 바로 찾을 수 있게 Map으로
        Map<Long, ParkingRecord> recordBySlotId = openRecords.stream()
                .collect(Collectors.toMap(
                        r -> r.getSlot().getId(),   // 키: 이 기록이 있는 면의 id
                        r -> r                      // 값: 기록 그 자체
                ));

        // 4. 면 하나 -> 응답 칸 하나로 바꾸기
        List<BoardResponse.Slot> slotResponses = slots.stream()
                .map(slot -> {
                    ParkingRecord record = recordBySlotId.get(slot.getId()); // 없으면 null

                    return new BoardResponse.Slot(
                            slot.getId().intValue(),
                            slot.getHousehold() == null ? null : slot.getHousehold().getUnitNumber(),
                            record == null ? "EMPTY" : "OCCUPIED",
                            record == null ? null : new BoardResponse.Occupant(
                                    record.getVehicle().getPlateNumber(),
                                    record.getEnteredAt()
                            )
                    );
                })
                .toList();

        return new BoardResponse(
                villa.getName(),
                villa.getLatitude(),
                villa.getLongitude(),
                slotResponses
        );
    }

    // ===== 22. 주차 현황판 (명세 전체) =====
    // 빌라 단위로 면·입차 기록·미확인·방문·가용시간을 한 번씩 조회해 Map으로 묶고, 면마다 Map에서 꺼내 씀 (N+1 방지)
    public ParkingBoardResponse getParkingBoard(Long userId) {
        User user = userRepository.findActiveWithHouseholdAndVilla(userId)
                .orElseThrow(() -> new BusinessException(NOT_IN_HOUSEHOLD));
        Household myHousehold = user.getHousehold();
        Villa villa = myHousehold.getVilla();
        boolean isAdmin = villa.getAdminUser() != null && villa.getAdminUser().getId().equals(user.getId());

        LocalDateTime now = LocalDateTime.now(KST).truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime tomorrowStart = todayStart.plusDays(1);

        // 1. 빌라 단위 조회 (쿼리 수가 면 수와 상관없이 일정)
        List<ParkingSlot> slots = parkingSlotRepository.findAllWithHouseholdByVillaId(villa.getId());
        Map<Long, ParkingRecord> recordBySlot = parkingRecordRepository.findOpenWithVehicleByVillaId(villa.getId())
                .stream().collect(Collectors.toMap(r -> r.getSlot().getId(), r -> r));
        Map<Long, UnidentifiedReport> reportBySlot = unidentifiedReportRepository.findOpenByVillaId(villa.getId())
                .stream().collect(Collectors.toMap(r -> r.getSlot().getId(), r -> r));
        Map<Long, List<Visit>> visitsBySlot = visitRepository.findBoardCandidatesByVillaId(villa.getId(), tomorrowStart)
                .stream().collect(Collectors.groupingBy(v -> v.getSlot().getId()));
        Map<Long, Availability> openAvailabilityBySlot = availabilityChecker.findOpenNowByVillaId(villa.getId(), now);

        // 2. 면 한 칸씩
        List<BoardSlotResponse> slotResponses = slots.stream()
                .map(slot -> {
                    boolean isCommon = slot.getHousehold() == null;
                    boolean isMine = !isCommon && slot.getHousehold().getId().equals(myHousehold.getId());
                    ParkingRecord record = recordBySlot.get(slot.getId());
                    UnidentifiedReport report = reportBySlot.get(slot.getId());
                    Visit visit = SlotStatusResolver.pickVisit(visitsBySlot.getOrDefault(slot.getId(), List.of()), now);
                    Availability availability = openAvailabilityBySlot.get(slot.getId());

                    SlotStatus status = SlotStatusResolver.resolve(report != null, record != null, visit, now, availability != null);
                    // 방문 상세는 관리자·면 주인·방문 세대에게만 (F8). 그 외 사용자는 status만 봄
                    boolean canSeeVisit = visit != null
                            && (isAdmin || isMine || visit.getHousehold().getId().equals(myHousehold.getId()));

                    return new BoardSlotResponse(
                            slot.getId(), slot.getLabel(),
                            slot.getX(), slot.getY(), slot.getW(), slot.getH(), slot.getRotation(),
                            isCommon,
                            isCommon ? null : slot.getHousehold().getUnitNumber(),
                            isMine,
                            status,
                            toOccupant(record, myHousehold),
                            availability == null ? null
                                    : new AvailableWindowResponse(availability.isAllDay(), availability.getStartTime(), availability.getEndTime()),
                            canSeeVisit ? BoardVisitResponse.from(visit) : null,
                            report == null ? null : report.getId()
                    );
                })
                .toList();

        // 3. 내 차: 열린 기록에서 차량 id → 면 id
        Map<Long, Long> slotByVehicle = recordBySlot.values().stream()
                .collect(Collectors.toMap(r -> r.getVehicle().getId(), r -> r.getSlot().getId()));
        List<BoardVehicleResponse> myVehicles = vehicleRepository.findByHouseholdIdOrderById(myHousehold.getId()).stream()
                .map(vehicle -> {
                    Long slotId = slotByVehicle.get(vehicle.getId());
                    return new BoardVehicleResponse(vehicle.getId(), vehicle.getPlateNumber(),
                            slotId == null ? VehicleState.DRIVING : VehicleState.PARKED, slotId);
                })
                .toList();

        // 4. 오늘 방문 (우리 세대)
        List<TodayVisitResponse> todayVisits = visitRepository
                .findTodayByHouseholdId(myHousehold.getId(), todayStart, tomorrowStart).stream()
                .map(v -> TodayVisitResponse.of(v, now))
                .toList();

        return new ParkingBoardResponse(now, villa.getLatitude() == null, slotResponses, myVehicles, todayVisits);
    }

    // 전체 번호는 내 세대 차량일 때만, 그 외에는 호수·뒤 4자리만 (F8)
    private OccupantResponse toOccupant(ParkingRecord record, Household myHousehold) {
        if (record == null) {
            return null;
        }
        Vehicle vehicle = record.getVehicle();
        boolean myVehicle = vehicle.getHousehold().getId().equals(myHousehold.getId());
        return new OccupantResponse(vehicle.getHousehold().getUnitNumber(), vehicle.getPlateLast4(),
                myVehicle ? vehicle.getPlateNumber() : null, record.getEnteredAt());
    }

    // 23. SSE 구독할 빌라 (세대 소속이 아니면 403)
    public Long findVillaId(Long userId) {
        return userRepository.findActiveWithHouseholdAndVilla(userId)
                .orElseThrow(() -> new BusinessException(NOT_IN_HOUSEHOLD))
                .getHousehold().getVilla().getId();
    }
}
