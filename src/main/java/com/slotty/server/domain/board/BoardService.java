package com.slotty.server.domain.board;

import com.slotty.server.domain.board.dto.BoardSlotResponse;
import com.slotty.server.domain.board.dto.ParkingBoardResponse;
import com.slotty.server.domain.parking.ParkingRecord;
import com.slotty.server.domain.parking.ParkingRecordRepository;
import com.slotty.server.domain.slot.ParkingSlot;
import com.slotty.server.domain.slot.ParkingSlotRepository;
import com.slotty.server.domain.user.User;
import com.slotty.server.domain.user.UserRepository;
import com.slotty.server.domain.vehicle.VehicleRepository;
import com.slotty.server.domain.villa.Household;
import com.slotty.server.domain.villa.Villa;
import com.slotty.server.global.exception.BusinessException;
import com.slotty.server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 조회만 함
public class BoardService {
    private final UserRepository userRepository;
    private final ParkingSlotRepository parkingSlotRepository;
    private final ParkingRecordRepository parkingRecordRepository;
    private final VehicleRepository vehicleRepository;

    public ParkingBoardResponse getBoard(Long userId) {

        // userId로 사용자, 세대, 빌라 정보 가져오기
        User user = userRepository.findActiveWithHouseholdAndVilla(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_IN_HOUSEHOLD));

        Household myHousehold = user.getHousehold();
        Villa villa = myHousehold.getVilla();

        // 면 목록과 열린 기록
        List<ParkingSlot> slots = parkingSlotRepository
                .findAllWithHouseholdByVillaId(villa.getId());
        List<ParkingRecord> openRecords = parkingRecordRepository
                .findOpenWithVehicleByVillaId(villa.getId());

        // 면 id로 기록 찾기 쉽게 Map 만듦
        Map<Long, ParkingRecord> openRecordBySlotId = openRecords.stream()
                .collect(Collectors.toMap(r -> r.getSlot().getId(), r -> r));

//        // 면마다 응답 만들기
//        List<BoardSlotResponse> slotResponses = slots.stream().map(
//                slot -> toSlotResponse(
//                        slot,
//                        openRecordBySlotId.get(slot.getId()),
//                        myHousehold.getId()
//                )
//        ).toList();

        // 내 차량
        Map<Long, Long> parkedSlotIdByVehicleId = openRecords.stream()
                .collect(Collectors.toMap(
                        r -> r.getVehicle().getId(),
                        r -> r.getSlot().getId()
                ));

        return null;
    }
}
