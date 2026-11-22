package com.slotty.server.domain.slot.service;

import com.slotty.server.domain.availability.AvailabilityChecker;
import com.slotty.server.domain.board.dto.AvailableWindowResponse;
import com.slotty.server.domain.parking.ParkingRecord;
import com.slotty.server.domain.parking.ParkingRecordRepository;
import com.slotty.server.domain.slot.ParkingSlot;
import com.slotty.server.domain.slot.ParkingSlotRepository;
import com.slotty.server.domain.slot.dto.SlotDetailResponse;
import com.slotty.server.domain.slot.dto.SlotDetailResponse.Occupant;
import com.slotty.server.domain.board.SlotStatusResolver;
import com.slotty.server.domain.board.dto.BoardVisitResponse;
import com.slotty.server.domain.slot.dto.SlotDetailResponse.Permissions;
import com.slotty.server.domain.visit.Visit;
import com.slotty.server.domain.visit.VisitRepository;
import com.slotty.server.domain.slot.dto.SlotDetailResponse.Unidentified;
import com.slotty.server.domain.unidentified.UnidentifiedReport;
import com.slotty.server.domain.unidentified.UnidentifiedReportRepository;
import com.slotty.server.domain.user.User;
import com.slotty.server.domain.user.UserRepository;
import com.slotty.server.domain.vehicle.Vehicle;
import com.slotty.server.domain.villa.Household;
import com.slotty.server.domain.villa.Villa;
import com.slotty.server.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static com.slotty.server.global.exception.ErrorCode.NOT_IN_HOUSEHOLD;
import static com.slotty.server.global.exception.ErrorCode.SLOT_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SlotService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final UserRepository userRepository;
    private final ParkingSlotRepository parkingSlotRepository;
    private final ParkingRecordRepository parkingRecordRepository;
    private final UnidentifiedReportRepository unidentifiedReportRepository;
    private final AvailabilityChecker availabilityChecker;
    private final VisitRepository visitRepository;

    // ===== 24. 면 상세 =====
    public SlotDetailResponse getSlotDetail(Long userId, Long slotId) {
        User user = userRepository.findActiveWithHouseholdAndVilla(userId)
                .orElseThrow(() -> new BusinessException(NOT_IN_HOUSEHOLD));
        Household myHousehold = user.getHousehold();
        Villa villa = myHousehold.getVilla();

        // 다른 빌라의 면이면 404 (존재 여부를 숨김)
        ParkingSlot slot = parkingSlotRepository.findWithHouseholdByIdAndVillaId(slotId, villa.getId())
                .orElseThrow(() -> new BusinessException(SLOT_NOT_FOUND));

        boolean isCommon = slot.getHousehold() == null;
        boolean isMine = !isCommon && slot.getHousehold().getId().equals(myHousehold.getId());
        boolean isAdmin = villa.getAdminUser() != null && villa.getAdminUser().getId().equals(user.getId());
        LocalDateTime now = LocalDateTime.now(KST);
        LocalDateTime tomorrowStart = now.toLocalDate().plusDays(1).atStartOfDay();

        ParkingRecord openRecord = parkingRecordRepository.findOpenWithVehicleBySlotId(slotId).orElse(null);
        UnidentifiedReport openReport = unidentifiedReportRepository.findBySlotIdAndResolvedAtIsNull(slotId).orElse(null);
        AvailableWindowResponse window = availabilityChecker.findOpenNow(slotId, now)
                .map(a -> new AvailableWindowResponse(a.isAllDay(), a.getStartTime(), a.getEndTime()))
                .orElse(null);
        Visit visit = SlotStatusResolver.pickVisit(visitRepository.findBoardCandidatesBySlotId(slotId, tomorrowStart), now);
        boolean canSeeVisit = visit != null
                && (isAdmin || isMine || visit.getHousehold().getId().equals(myHousehold.getId()));

        return new SlotDetailResponse(
                slot.getId(),
                slot.getLabel(),
                isCommon,
                isCommon ? null : slot.getHousehold().getUnitNumber(),
                isMine,
                SlotStatusResolver.resolve(openReport != null, openRecord != null, visit, now, window != null),
                toOccupant(openRecord, myHousehold),
                window,
                canSeeVisit ? BoardVisitResponse.from(visit) : null,
                toUnidentified(openReport, isAdmin || isMine),
                new Permissions(
                        // 등록 차량이 입차해 있는 면은 미확인이 아님 (43번도 409 SLOT_OCCUPIED)
                        (isAdmin || isMine) && openReport == null && openRecord == null,
                        (isAdmin || isMine) && openReport != null,
                        isMine && openReport != null
                )
        );
    }

    // 전체 번호는 내 세대 차량일 때만, 그 외에는 호수·뒤 4자리만 (F8 개인정보)
    private Occupant toOccupant(ParkingRecord record, Household myHousehold) {
        if (record == null) {
            return null;
        }
        Vehicle vehicle = record.getVehicle();
        boolean myVehicle = vehicle.getHousehold().getId().equals(myHousehold.getId());
        return new Occupant(
                vehicle.getHousehold().getUnitNumber(),
                vehicle.getPlateLast4(),
                myVehicle ? vehicle.getPlateNumber() : null,
                record.getEnteredAt()
        );
    }

    // 미확인 차량 번호는 관리자·면 주인에게만
    private Unidentified toUnidentified(UnidentifiedReport report, boolean canSeePlate) {
        if (report == null) {
            return null;
        }
        return new Unidentified(report.getId(), canSeePlate ? report.getPlateNumber() : null, report.getReportedAt());
    }
}
