package com.slotty.server.domain.slot.service;

import com.slotty.server.domain.availability.AvailabilityChecker;
import com.slotty.server.domain.board.SlotStatus;
import com.slotty.server.domain.board.dto.AvailableWindowResponse;
import com.slotty.server.domain.parking.ParkingRecord;
import com.slotty.server.domain.parking.ParkingRecordRepository;
import com.slotty.server.domain.slot.ParkingSlot;
import com.slotty.server.domain.slot.ParkingSlotRepository;
import com.slotty.server.domain.slot.dto.SlotDetailResponse;
import com.slotty.server.domain.slot.dto.SlotDetailResponse.Occupant;
import com.slotty.server.domain.slot.dto.SlotDetailResponse.Permissions;
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

        ParkingRecord openRecord = parkingRecordRepository.findOpenWithVehicleBySlotId(slotId).orElse(null);
        UnidentifiedReport openReport = unidentifiedReportRepository.findBySlotIdAndResolvedAtIsNull(slotId).orElse(null);
        AvailableWindowResponse window = availabilityChecker.findOpenNow(slotId, now)
                .map(a -> new AvailableWindowResponse(a.isAllDay(), a.getStartTime(), a.getEndTime()))
                .orElse(null);

        return new SlotDetailResponse(
                slot.getId(),
                slot.getLabel(),
                isCommon,
                isCommon ? null : slot.getHousehold().getUnitNumber(),
                isMine,
                statusOf(openReport, openRecord, window),
                toOccupant(openRecord, myHousehold),
                window,
                toUnidentified(openReport, isAdmin || isMine),
                new Permissions(
                        (isAdmin || isMine) && openReport == null,
                        (isAdmin || isMine) && openReport != null,
                        isMine && openReport != null
                )
        );
    }

    // 상태 우선순위 (명세 22번 표): UNIDENTIFIED > OCCUPIED > (방문 상태는 방문 기능 추가 시) > AVAILABLE > EMPTY
    private SlotStatus statusOf(UnidentifiedReport openReport, ParkingRecord openRecord, AvailableWindowResponse window) {
        if (openReport != null) {
            return SlotStatus.UNIDENTIFIED;
        }
        if (openRecord != null) {
            return SlotStatus.OCCUPIED;
        }
        if (window != null) {
            return SlotStatus.AVAILABLE;
        }
        return SlotStatus.EMPTY;
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
