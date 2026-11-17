package com.slotty.server.domain.board.service;

import com.slotty.server.domain.board.dto.BoardResponse;
import com.slotty.server.domain.slot.ParkingSlot;
import com.slotty.server.domain.slot.ParkingSlotRepository;
import com.slotty.server.domain.user.User;
import com.slotty.server.domain.user.UserRepository;
import com.slotty.server.domain.villa.Villa;
import com.slotty.server.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import static com.slotty.server.global.exception.ErrorCode.NOT_IN_HOUSEHOLD;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 조회만 함
public class BoardService {
    private final UserRepository userRepository;
    private final ParkingSlotRepository parkingSlotRepository;

    public BoardResponse getBoard(Long userId) {

        // 1. 사용자 + 세대 + 빌라를 한 번에 조회, 없으면 403
        User user = userRepository.findActiveWithHouseholdAndVilla(userId)
                .orElseThrow(() -> new BusinessException(NOT_IN_HOUSEHOLD)); // 403

        // 2. 빌라 꺼내기
        Villa villa = user.getHousehold().getVilla();

        // 3. 이 빌라의 면 목록
        List<ParkingSlot> slots = parkingSlotRepository
                .findAllWithHouseholdByVillaId(villa.getId());

        // 4. 면 하나 -> 응답 칸 하나로 바꾸기
        List<BoardResponse.Slot> slotResponses = slots.stream()
                .map((ParkingSlot slot) -> new BoardResponse.Slot(
                        slot.getId().intValue(),
                        slot.getHousehold() == null ? null : slot.getHousehold().getUnitNumber(),
                        "EMPTY",
                        null
                )).toList();

        return new BoardResponse(
                villa.getName(),
                slotResponses
        );
    }
}
