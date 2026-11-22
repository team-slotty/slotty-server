package com.slotty.server.domain.slot.service;

import com.slotty.server.domain.availability.AvailabilityExceptionRepository;
import com.slotty.server.domain.availability.AvailabilityRepository;
import com.slotty.server.domain.slot.ParkingSlot;
import com.slotty.server.domain.slot.ParkingSlotRepository;
import com.slotty.server.domain.slot.dto.SlotAssignmentRequest;
import com.slotty.server.domain.villa.AdminChecker;
import com.slotty.server.domain.villa.Household;
import com.slotty.server.domain.villa.HouseholdRepository;
import com.slotty.server.domain.villa.Villa;
import com.slotty.server.domain.visit.VisitSlotPolicy;
import com.slotty.server.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

import static com.slotty.server.global.exception.ErrorCode.HOUSEHOLD_NOT_FOUND;
import static com.slotty.server.global.exception.ErrorCode.SLOT_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class SlotAssignmentService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final AdminChecker adminChecker;
    private final ParkingSlotRepository parkingSlotRepository;
    private final HouseholdRepository householdRepository;
    private final AvailabilityRepository availabilityRepository;
    private final AvailabilityExceptionRepository availabilityExceptionRepository;
    private final VisitSlotPolicy visitSlotPolicy;

    // ===== 21. 세대별 면 배정 (보낸 면만, 하나라도 실패하면 전부 취소) =====
    @Transactional
    public void assign(Long userId, SlotAssignmentRequest request) {
        Villa villa = adminChecker.requireAdmin(userId).getHousehold().getVilla();

        for (SlotAssignmentRequest.Assignment assignment : request.assignments()) {
            ParkingSlot slot = parkingSlotRepository.findWithHouseholdByIdAndVillaId(assignment.slotId(), villa.getId())
                    .orElseThrow(() -> new BusinessException(SLOT_NOT_FOUND));
            Household newOwner = assignment.householdId() == null ? null
                    : householdRepository.findByIdAndVillaId(assignment.householdId(), villa.getId())
                    .orElseThrow(() -> new BusinessException(HOUSEHOLD_NOT_FOUND));

            Long oldOwnerId = slot.getHousehold() == null ? null : slot.getHousehold().getId();
            Long newOwnerId = newOwner == null ? null : newOwner.getId();
            if (!Objects.equals(oldOwnerId, newOwnerId)) {
                // 주인이 바뀌거나 공용이 되면 이전 주인이 등록한 가용시간은 의미가 없어 삭제 (명세 부수 효과)
                availabilityExceptionRepository.deleteAllBySlotId(slot.getId());
                availabilityRepository.deleteAllBySlotId(slot.getId());
                slot.assignTo(newOwner);
                // 가용시간이 없어졌으니 그걸 근거로 지정한 방문은 취소 표시 (공용이 됐으면 그대로 지정 가능해서 취소 안 됨)
                visitSlotPolicy.cancelBrokenVisits(slot, LocalDateTime.now(KST));
            }
        }
    }
}
